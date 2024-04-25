package it.dii.unipi.masss.noisemapper

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
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
    }

    fun setupProximityManager(activity: BLEScannerActivity){
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
                activity.findViewById<ListView>(R.id.beacon_list).adapter = BeaconAdapter(beacons)
            }
        })
    }

    class BeaconAdapter(private val beacons: MutableList<IBeaconDevice>) : BaseAdapter() {

        override fun getCount(): Int {
            return beacons.size
        }

        override fun getItem(position: Int): Any {
            return beacons[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val beacon = beacons[position]
            val view = convertView ?: LayoutInflater.from(parent?.context).inflate(R.layout.beacon_item, parent, false)
            view.findViewById<TextView>(R.id.beacon_id).text = beacon.uniqueId
            view.findViewById<TextView>(R.id.beacon_distance).text = beacon.distance.toString()
            return view
        }

    }

    fun stopScanning() {
        proximityManager.stopScanning()
    }

    fun startScanning() {
        proximityManager.connect {
            proximityManager.startScanning()
        }
    }

}