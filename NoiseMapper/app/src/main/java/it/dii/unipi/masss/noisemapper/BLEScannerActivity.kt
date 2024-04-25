package it.dii.unipi.masss.noisemapper

import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kontakt.sdk.android.common.KontaktSDK

class BLEScannerActivity : AppCompatActivity() {
    private val BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE = 102
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ble_layout)
        KontaktSDK.initialize(this);
        val button: Button = findViewById(R.id.stop_ble)
        button.setOnClickListener {
            // Create an Intent to return to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Close the current activity
            onStop()
        }
        requestPermissions()
        val ble_scanner = BLEScanner(this)
        ble_scanner.startScanning()
    }

    private fun requestPermissions() {
        val selfPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (selfPermission != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE
            )
        } else {
            println("iBeacon: Starting scanning")

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
            BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d("BluetoothRequest", "Permission granted")

                } else {
                    Log.d("iBeacon",  "Permission not granted")
                }
            }
        }
    }

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
