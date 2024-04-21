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
import kotlin.math.abs
import kotlin.math.log10


class NoiseDetection : AppCompatActivity(), SensorEventListener {
    private val map_noise_level = mutableMapOf<Long , Double>()
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 101
    private val OUTPUT_FORMAT_AUDIO = MediaRecorder.OutputFormat.MPEG_4
    private val AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
    private val AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_PERFORMANCE
    private val AUDIO_ENCODING_BIT_RATE = 16*44100
    private val AUDIO_SAMPLING_RATE = 44100
    private val REFRESH_RATE = 500
    private var mRecorder : MediaRecorder? = null
    private val timer = Timer()
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var isNearObject = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.noise_detection)
        val button: Button = findViewById(R.id.stop_button)
        button.setOnClickListener {
            // Create an Intent to return to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Close the current activity
            onStop()
        }
        requestPermission()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
            )
        } else {
            noise_sampling()
            Log.d("MicrophoneRequest", "Permission already granted")
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RECORD_AUDIO_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d("MicrophoneRequest", "Permission granted")
                    noise_sampling()
                } else {
                    val resultTextView: TextView = findViewById(R.id.upper_text_noise_activity)
                    resultTextView.text = getString(R.string.microphone_request_not_granted)
                }
            }
        }
    }

    private fun noise_sampling() {
        mRecorder = MediaRecorder()
        mRecorder!!.setAudioSource(AUDIO_SOURCE)
        mRecorder!!.setOutputFormat(OUTPUT_FORMAT_AUDIO)
        mRecorder!!.setAudioEncoder(AUDIO_ENCODER)
        mRecorder!!.setAudioEncodingBitRate(AUDIO_ENCODING_BIT_RATE);
        mRecorder!!.setAudioSamplingRate(AUDIO_SAMPLING_RATE);
        mRecorder!!.setOutputFile(FileOutputStream(File(cacheDir, "audio.mp3")).fd)
        timer.scheduleAtFixedRate(RecorderTask(mRecorder!!), 0, REFRESH_RATE.toLong())
        try {
            mRecorder!!.prepare()
            mRecorder!!.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private inner class RecorderTask(private val recorder: MediaRecorder) : TimerTask() {
        private val sound = findViewById<TextView>(R.id.db_level)

        override fun run() {
            runOnUiThread {
                val amplitude = recorder.maxAmplitude
                var amplitudeDb = 20 * log10(abs(amplitude).toDouble())
                if (isNearObject) {
                    amplitudeDb -= 10 // TODO: calibrate this value
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
    override fun onDestroy() {
        super.onDestroy()
        mRecorder?.stop()
        mRecorder?.release()
        timer.cancel()
        mRecorder = null
    }

    override fun onPause() {
        super.onPause()
        mRecorder?.stop()
        mRecorder?.release()
        timer.cancel()
        mRecorder = null
        sensorManager?.unregisterListener(this)
    }
    override fun onResume() {
        super.onResume()
        noise_sampling()
        sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onStop() {
        super.onStop()
        mRecorder?.stop()
        mRecorder?.release()
        timer.cancel()
        mRecorder = null
        finish()
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
