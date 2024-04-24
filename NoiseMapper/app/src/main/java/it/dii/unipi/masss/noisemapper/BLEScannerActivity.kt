package it.dii.unipi.masss.noisemapper

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kontakt.sdk.android.common.KontaktSDK

class BLEScannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ble_layout)
        KontaktSDK.initialize(this);
        var ble_scanner = BLEScanner(this)
        val button: Button = findViewById(R.id.stop_ble)
        button.setOnClickListener {
            // Create an Intent to return to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Close the current activity
            onStop()
        }
        //requestPermissions()
        ble_scanner.startScanning()
    }

    /*
    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE
            )
        } else {
            println("iBeacon: Starting scanning")

        }
    }
     */

    override fun onStart() {
        super.onStart()
        println("iBeacon: Starting scanning")
    }

    override fun onStop() {
        super.onStop()
        println("iBeacon: Stopping scanning")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("iBeacon: Stopping scanning")
    }
}
