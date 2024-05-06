package it.dii.unipi.masss.noisemapper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.widget.Switch
import androidx.appcompat.widget.SwitchCompat
import com.kontakt.sdk.android.common.KontaktSDK
import kotlin.math.abs
import kotlin.math.log10


class NoiseDetection : AppCompatActivity(), SensorEventListener {
    lateinit var bleConfig: BLEConfig
    private var gotConfig: Boolean = false
    val map_noise_level = mutableMapOf<Long , Double>()
    private val RECORD_AUDIO_BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE = 101
    private val OUTPUT_FORMAT_AUDIO = MediaRecorder.OutputFormat.MPEG_4
    private val AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
    private val AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_PERFORMANCE
    private val AUDIO_ENCODING_BIT_RATE = 16*44100
    private val AUDIO_SAMPLING_RATE = 44100
    private val REFRESH_RATE = 500
    private val DB_ADJUSTMENT_PROXIMITY_SENSOR = 10
    private var mRecorder : MediaRecorder? = null
    private var timer = Timer()
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var isNearObject = false
    private var ble_scanner : BLEScanner? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ble_layout)
        val button: Button = findViewById(R.id.stop_ble)
        button.setOnClickListener {
            // Create an Intent to return to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Close the current activity
            onStop()
        }


        // call the class to read the BLEConfig file
        bleConfig = BLEConfig(this.applicationContext)
        gotConfig = bleConfig.getConfig()
        if (gotConfig){
            //println(bleConfig.beaconRoomMap["beacon1"]) // should print "room1"
            // Initialize Kontakt SDK
            KontaktSDK.initialize(this);
            ble_scanner = BLEScanner(this)
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        requestPermissions()
        val switch1 : SwitchCompat = findViewById<SwitchCompat>(R.id.switch1)
        switch1.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
                val pollingRequest = PollingRequest(this)
                pollingRequest.start()
            } else {
                Log.i("NoiseDetection", "Switch is off")
            }
        }


    }

    private fun requestPermissions() {
        val requiredPermissions =
            arrayOf(Manifest.permission.RECORD_AUDIO) +
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION)
            else arrayOf<String>(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) // Note that there is no need to ask about ACCESS_FINE_LOCATION anymore for BT scanning purposes for VERSION_CODES.S and higher if you add android:usesPermissionFlags="neverForLocation" under BLUETOOTH_SCAN in your manifest file.

        // check if the permissions are already granted
        val notGrantedPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (notGrantedPermissions.isNotEmpty()) {
            // request the permissions
            ActivityCompat.requestPermissions(
                this,
                notGrantedPermissions,
                RECORD_AUDIO_BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE
            )
        } else {
            println("iBeacon: Permissions already granted, starting scanning")
            startSensing()
        }
        // check that bluetooth is enabled, if not, ask the user to enable it
        val bluetoothAdapter = android.bluetooth.BluetoothManager::class.java.cast(
            getSystemService(android.content.Context.BLUETOOTH_SERVICE)
        )?.adapter
        if (!(bluetoothAdapter?.isEnabled)!!) {
            val enableBtIntent = Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i("BluetoothRequest", "Request code is $requestCode")
        when (requestCode) {
            RECORD_AUDIO_BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults.filter {
                        it == PackageManager.PERMISSION_DENIED
                    }.toTypedArray().isEmpty())) {
                    Log.d("PermissionRequest", "All permissions granted")
                    startSensing()
                } else {
                    Log.d("iBeacon",  "Permission not granted")
                }
            }
        }
    }

    private fun startSensing(){
        Log.d("MicrophoneRequest", "Permission granted")
        initializeMediaRecorder()
        noise_sampling()
        ble_scanner?.startScanning()
    }
    private fun initializeMediaRecorder(){
        mRecorder = MediaRecorder()
        mRecorder!!.setAudioSource(AUDIO_SOURCE)
        mRecorder!!.setOutputFormat(OUTPUT_FORMAT_AUDIO)
        mRecorder!!.setAudioEncoder(AUDIO_ENCODER)
        mRecorder!!.setAudioEncodingBitRate(AUDIO_ENCODING_BIT_RATE);
        mRecorder!!.setAudioSamplingRate(AUDIO_SAMPLING_RATE);
        mRecorder!!.setOutputFile(FileOutputStream(File(cacheDir, "audio.mp3")).fd)
    }
    private fun noise_sampling() {
        try {
            mRecorder!!.prepare()
            mRecorder!!.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        timer.scheduleAtFixedRate(RecorderTask(mRecorder!!), 0, REFRESH_RATE.toLong())
    }
    private inner class RecorderTask(private val recorder: MediaRecorder) : TimerTask() {
        private val sound = findViewById<TextView>(R.id.db_level)

        override fun run() {
            runOnUiThread {
                val amplitude = recorder.maxAmplitude
                Log.i("NoiseDetection", "Recorder max amplitude is $amplitude")
                var amplitudeDb = 20 * log10(abs(if (amplitude==0) 1 else amplitude).toDouble())
                if (isNearObject) {
                    amplitudeDb += DB_ADJUSTMENT_PROXIMITY_SENSOR // TODO: calibrate this value
                    Log.i("NoiseDetection", "Proximity sensor detected an object")
                }
                val currentTimestamp = System.currentTimeMillis()
                map_noise_level[currentTimestamp] = amplitudeDb
                Log.i("NoiseDetection", "Level db is $amplitudeDb at time $currentTimestamp")
                sound.text = String.format("%.1f", amplitudeDb)
                when {
                    amplitudeDb > 80 -> { // High noise level
                        sound.setTextColor(ContextCompat.getColor(this@NoiseDetection, R.color.high_noise))
                    }
                    amplitudeDb > 60 -> { // Medium noise level
                        sound.setTextColor(ContextCompat.getColor(this@NoiseDetection, R.color.medium_noise))
                    }
                    else -> { // Low noise level
                        sound.setTextColor(ContextCompat.getColor(this@NoiseDetection, R.color.low_noise))
                    }
                }

            }
        }
    }
/*
    override fun onPause() {
        super.onPause()
        mRecorder?.stop()
        timer.cancel()
        sensorManager?.unregisterListener(this)
    }
*/
    override fun onStart() {
        super.onStart()
        // if the switch is checked, start the BLE scanning
        val switch1: SwitchCompat = findViewById(R.id.switch1)
        if (switch1.isChecked) {
            sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
            val pollingRequest = PollingRequest(this)
            pollingRequest.start()
        }
    }

    override fun onStop() {
        super.onStop()
        timer.cancel()
        sensorManager?.unregisterListener(this)
        mRecorder?.stop()
        mRecorder?.release()
        mRecorder = null
    }

    override fun onRestart() {
        super.onRestart()
        timer = Timer()
        requestPermissions()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            isNearObject = event.values[0] < (proximitySensor?.maximumRange ?: 0.0f)
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

}
