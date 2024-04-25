package it.dii.unipi.masss.noisemapper

import android.content.pm.PackageManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kontakt.sdk.android.ble.device.BeaconRegion
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener
import com.kontakt.sdk.android.common.profile.IBeaconDevice
import com.kontakt.sdk.android.common.profile.IBeaconRegion
import java.util.Collections
import java.util.UUID

class BLEScanner (activity: BLEScannerActivity) {
    private val proximityManager = ProximityManagerFactory.create(activity)

    init {
        setupProximityManager(activity)
        //setupSpaces()
    }

    fun setupProximityManager(activity: BLEScannerActivity){
        proximityManager.setIBeaconListener(object : SimpleIBeaconListener() {
            override fun onIBeaconDiscovered(device: IBeaconDevice, region: IBeaconRegion) {
                // print the discovered iBeacon device
                println("iBeacon: Discovered iBeacon device: $device")
                activity.findViewById<TextView>(R.id.textView2).text = "new device discovered: ${device.uniqueId}"
            }

            override fun onIBeaconLost(device: IBeaconDevice, region: IBeaconRegion) {
                // print the lost iBeacon device
                println("iBeacon: Lost iBeacon device: $device")
            }

            override fun onIBeaconsUpdated(beacons: MutableList<IBeaconDevice>, region: IBeaconRegion) {
                // print the updated iBeacon devices
                println("iBeacon: Updated iBeacon devices: $beacons")
                // concatenate the unique IDs of the updated iBeacon devices

            }
        })
    }

    /*
    fun setupSpaces()
    {
        //Setting up single iBeacon region. Put your own desired values here.
        val region : IBeaconRegion = BeaconRegion.Builder()
            .identifier("My Region") //Region identifier is mandatory.
            .proximity(UUID.fromString("f7826da6-4fa2-4e98-8024-bc5b71e0893e")) //Default Kontakt.io proximity.
            //Optional major and minor values
            //.major(1)
            //.minor(1)
            .build();

        proximityManager.spaces().iBeaconRegion(region)
            .forceResolveRegions(Collections.singleton(UUID.fromString("f7826da6-4fa2-4e98-8024-bc5b71e0893e")));
    }

     */


    fun stopScanning() {
        proximityManager.stopScanning()
    }

    fun startScanning() {
        proximityManager.connect {
            proximityManager.startScanning()
        }
    }

}