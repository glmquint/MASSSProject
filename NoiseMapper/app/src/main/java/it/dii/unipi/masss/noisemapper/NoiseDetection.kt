package it.dii.unipi.masss.noisemapper

import android.Manifest
import android.content.Intent
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


class NoiseDetection : AppCompatActivity() {
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 101
    private val REFRESH_RATE = 500
    private var mRecorder : MediaRecorder? = null
    private val timer = Timer()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.noise_detection)
        val button: Button = findViewById(R.id.stop_button)
        requestPermission()
        button.setOnClickListener {
            // Create an Intent to return to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Close the current activity
            onDestroy()
        }
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
        mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
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
                val amplitudeDb = 20 * Math.log10(Math.abs(amplitude).toDouble())
                Log.i("NoiseDetection", "Level db is $amplitudeDb")
                sound.text = String.format("%.1f", amplitudeDb)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mRecorder?.stop()
        mRecorder?.release()
        timer.cancel()
    }

    override fun onPause() {
        super.onPause()
        mRecorder?.stop()
        mRecorder?.release()
        timer.cancel()
    }
    override fun onResume() {
        super.onResume()
        noise_sampling()

    }

}
