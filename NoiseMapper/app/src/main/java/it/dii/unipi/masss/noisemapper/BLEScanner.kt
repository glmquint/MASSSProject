package it.dii.unipi.masss.noisemapper
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener
import com.kontakt.sdk.android.common.profile.IBeaconDevice
import com.kontakt.sdk.android.common.profile.IBeaconRegion
import it.dii.unipi.masss.noisemapper.MainActivity
import it.dii.unipi.masss.noisemapper.R

class BLEScanner : AppCompatActivity() {
    private val proximityManager = ProximityManagerFactory.create(this)
    private val BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ble_layout)
        // get the stop_ble button
        val button: Button = findViewById(R.id.stop_ble)
        button.setOnClickListener {
            // Create an Intent to return to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Close the current activity
            onStop()
        }
        requestPermissions()


        proximityManager.setIBeaconListener(object : SimpleIBeaconListener() {
            override fun onIBeaconDiscovered(device: IBeaconDevice, region: IBeaconRegion) {
                // print the discovered iBeacon device
                println("iBeacon: Discovered iBeacon device: $device")
            }

            override fun onIBeaconLost(device: IBeaconDevice, region: IBeaconRegion) {
                // print the lost iBeacon device
                println("iBeacon: Lost iBeacon device: $device")
            }

            override fun onIBeaconsUpdated(beacons: MutableList<IBeaconDevice>, region: IBeaconRegion) {
                // print the updated iBeacon devices
                println("iBeacon: Updated iBeacon devices: $beacons")
            }
        })
    }

    private fun requestPermissions(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
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
            proximityManager.connect {
                proximityManager.startScanning()
            }

            Log.d("MicrophoneRequest", "Permission already granted")
        }

    }
    override fun onStart() {
        super.onStart()
        println("iBeacon: Starting scanning")
        proximityManager.connect {
            proximityManager.startScanning()
        }
    }

    override fun onStop() {
        super.onStop()
        proximityManager.stopScanning()
    }

    override fun onDestroy() {
        super.onDestroy()
        proximityManager.startScanning()
    }
}
