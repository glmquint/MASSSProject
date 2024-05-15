package it.dii.unipi.masss.noisemapper

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.MaterialDatePicker

import androidx.core.util.Pair
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.fixedRateTimer
import kotlin.math.abs
import kotlin.math.log10

class NoiseActivity() : AppCompatActivity() {
    lateinit var url: String

    //private lateinit var  url : String
    private lateinit var webviewUpdateTimer: Timer
    val map_noise_level: MutableMap<Long, Double> = mutableMapOf()
    private lateinit var noise_microphone: NoiseMicrophone
    private lateinit var ble_scanner: BLEScanner
    private lateinit var pickDateButton: Button
    private val RECORD_AUDIO_BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE = 101
    private lateinit var switchCompat: SwitchCompat
    private lateinit var webView: WebView
    private lateinit var graph: Graph
    private lateinit var updateMapTimer: Timer
    lateinit var bleConfig: BLEConfig
    private lateinit var noise_map_io: NoiseMapIO
    private lateinit var notGrantedPermissions : Array<String>
    private val requiredPermissions = arrayOf(Manifest.permission.RECORD_AUDIO) +
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                else arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) // Note that there is no need to ask about ACCESS_FINE_LOCATION anymore for BT scanning purposes for VERSION_CODES.S and higher if you add android:usesPermissionFlags="neverForLocation" under BLUETOOTH_SCAN in your manifest file.
    private var bluetoothAdapter : BluetoothAdapter? = null
    var startDate:Long = 0
    var endDate:Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.noise_activity)
        // get the URL from the intent
        url = intent.getStringExtra("serverURL").toString()
        Log.i("NoiseActivity", "URL is $url")
        Log.i("NoiseActivity", "Noise activity started")
        bleConfig = BLEConfig(this, serverUrl = url)
        if (!bleConfig.gotConfig()){
            Toast.makeText(
                this,
                "Please connect to Internet for the first app lunch",
                Toast.LENGTH_LONG
            ).show()
            intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            return
        }
        noise_map_io = NoiseMapIO(this, url)
        createGraphUI()
        bluetoothAdapter = android.bluetooth.BluetoothManager::class.java.cast(
            getSystemService(android.content.Context.BLUETOOTH_SERVICE)
        )?.adapter
        startDate = System.currentTimeMillis()-this.resources.getInteger(R.integer.MILLISECONDS_IN_A_WEEK)
        endDate = System.currentTimeMillis()
        pickDateButton = findViewById(R.id.pick_date_button)
        pickDateButton.setOnClickListener {
            val dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Select dates")
                    .setSelection(
                        Pair(
                            startDate,
                            endDate
                        )
                    )
                    .build()
            dateRangePicker.addOnPositiveButtonClickListener { selection ->
                startDate = selection.first
                endDate = selection.second
                Log.i("MainActivity", "Date range selected: $startDate - $endDate")
                updateMap(startDate, endDate)
            }
            if (inSensingState()) {
                updateMap()
            } else {
                dateRangePicker.show(supportFragmentManager, "dateRangePicker")
            }
        }

        // register for the switch compat change event
        switchCompat.setOnCheckedChangeListener { _, isChecked ->
            Log.i("NoiseMapper","Switch event")
            if (isChecked){
                Log.i("NoiseMapper","Switch is ON: entering in sensing state")
                enterSensingState()
            }else{
                Log.i("NoiseMapper","Switch is OFF: exiting sensing state")
                exitSensingState()
            }
        }
    }

    fun createGraphUI(){
        graph = Graph(filesDir.absolutePath, bleConfig)
        webView = findViewById(R.id.map_web_view)
        switchCompat = findViewById(R.id.sensing_on_off)
        // create the graph
        val timeAnHourBefore = Calendar.getInstance()
        timeAnHourBefore.add(Calendar.HOUR_OF_DAY, -1*24*7*4) // TODO: nocheckin
        updateMap(startTS = timeAnHourBefore.timeInMillis)

        webView.settings.javaScriptEnabled = true;
        webView.settings.allowFileAccess = true;
        webView.settings.builtInZoomControls = true;
        //webView.webViewClient = WebViewClient()
        webView.loadUrl("file://" + filesDir.absolutePath + "/output.html")
    }
    private fun inSensingState(): Boolean {
        return findViewById<SwitchCompat>(R.id.sensing_on_off).isChecked
    }

    fun enterSensingState(){
        pickDateButton.text = getString(R.string.refresh_map)
        Log.i("NoiseMapper","Entering sensing state")
        while (!permissionCheck()) {
            requestPermissions()
        }
        enableBT()
        initializeSensors()
        startSensing()
        scheduleUpdate()
    }

    @SuppressLint("MissingPermission")
    private fun enableBT() {
        // check that bluetooth is enabled, if not, ask the user to enable it
        if (!(bluetoothAdapter?.isEnabled)!!) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            // start the intent to enable bluetooth with a callback for when the user enables it

            startActivity(enableBtIntent) // HACK: look for a way to get the result of the intent
            // TODO: probably use startActivityForResult instead of startActivity
        }
    }

    private fun startSensing() {
        noise_microphone.startListening()
        ble_scanner.startScanning()
    }

    inner class RecorderTask() : TimerTask() {

        private lateinit var sound: TextView
        private lateinit var mRecorder: MediaRecorder

        // /!!\ This setup method must be called before the TimerTask is scheduled /!!\
        // We cannot use the default constructor because those variables are passed by NoiseMicrophone
        fun setup(mRecorder: MediaRecorder, sound: TextView) {
            this.mRecorder = mRecorder
            this.sound = sound
        }
        override fun run() {
            runOnUiThread {
                val amplitude = mRecorder.maxAmplitude
                Log.i("NoiseDetection", "Recorder max amplitude is $amplitude")
                var amplitudeDb = 20 * log10(abs(if (amplitude==0) 1 else amplitude).toDouble())
                /*
                if (isNearObject) {
                    amplitudeDb += DB_ADJUSTMENT_PROXIMITY_SENSOR // TODO: calibrate this value
                    Log.i("NoiseDetection", "Proximity sensor detected an object")
                }
                */
                val currentTimestamp = System.currentTimeMillis()
                map_noise_level[currentTimestamp] = amplitudeDb
                Log.i("NoiseDetection", "Level db is $amplitudeDb at time $currentTimestamp")
                sound.text = String.format("%.1f dB", amplitudeDb)
                when {
                    amplitudeDb > 80 -> { // High noise level
                        sound.setTextColor(ContextCompat.getColor(this@NoiseActivity, R.color.high_noise))
                    }
                    amplitudeDb > 60 -> { // Medium noise level
                        sound.setTextColor(ContextCompat.getColor(this@NoiseActivity, R.color.medium_noise))
                    }
                    else -> { // Low noise level
                        sound.setTextColor(ContextCompat.getColor(this@NoiseActivity, R.color.low_noise))
                    }
                }
            }
        }
    }

    private fun initializeSensors() {
        while (!(bluetoothAdapter?.isEnabled)!!) {
            continue // HACK: if the user doesn't enable the bluetooth, the app will be stuck here forever
        }
        val recorderTask = RecorderTask()
        noise_microphone = NoiseMicrophone(this,cacheDir, findViewById(R.id.db_level), recorderTask)
        ble_scanner = BLEScanner(this)
    }


    private fun permissionCheck(): Boolean {
        // check if the permissions are already granted
        notGrantedPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        return notGrantedPermissions.isEmpty()
    }

    private fun requestPermissions(){
        if (notGrantedPermissions.isEmpty()) {
            Log.w("NoiseMapper", "Permissions already granted")
            return
        }
        // request the permissions
        ActivityCompat.requestPermissions(
            this,
            notGrantedPermissions,
            RECORD_AUDIO_BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE
        )
        return
    }

    private fun scheduleUpdate() {
        // start a timer
        updateMapTimer = fixedRateTimer(initialDelay = 2000, period = 10*1000) {
            updateMap()
        }
    }

    private fun updateMap(startTS : Long? = null, endTS : Long? = null) {
        val currentTime = Calendar.getInstance()

        // Get time an hour before
        val timeAnHourBefore = Calendar.getInstance()
        timeAnHourBefore.add(Calendar.HOUR_OF_DAY, -1)

        // Convert to Unix timestamp
        val currentUnixTimestamp = endTS?:currentTime.timeInMillis
        val previousHourUnixTimestamp = startTS?:timeAnHourBefore.timeInMillis

        // Log the results
        Log.i("PollingRequest", "Current time: ${currentTime.time}")
        Log.i("PollingRequest", "Unix timestamp of current time: $currentUnixTimestamp")

        Log.i("PollingRequest", "Time an hour before: ${timeAnHourBefore.time}")
        Log.i(
            "PollingRequest",
            "Unix timestamp of time an hour before: $previousHourUnixTimestamp"
        )
        graph.makeplot(noise_map_io.performGetRequest(previousHourUnixTimestamp / 1000, currentUnixTimestamp / 1000))
        runOnUiThread {
            webView.loadUrl("file://" + filesDir.absolutePath + "/output.html")
        }
    }

    private fun exitSensingState(){
        pickDateButton.text = getString(R.string.pick_date)
        Log.i("NoiseMapper","Exiting sensing state")
        stopSensing()
        stopUpdate()
    }

    private fun stopSensing() {
        noise_microphone.stopListening()
        ble_scanner.stopScanning()
        // reset the text value of the noise indicator
        val sound : TextView = findViewById(R.id.db_level)
        sound.text = getString(R.string.db_level_no_sensing)
        sound.setTextColor(findViewById<TextView>(R.id.current_room).currentTextColor)

    }

    private fun stopUpdate() {
        updateMapTimer.cancel()
    }

    override fun onStop(){
        super.onStop()
        if (inSensingState()){
            switchCompat.isChecked = false
        }
    }
}

