package it.dii.unipi.masss.noisemapper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.util.Log

class PowerSaveModeDetector(private val context: Context) {
    var isPowerSaveMode: Boolean = false


    private val powerSaveModeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            when (intent.action) {
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    if (powerManager.isPowerSaveMode) {
                        // Device entered power save mode
                        Log.i("PowerSaveModeDetector", "Power save mode ON")
                        isPowerSaveMode = true
                    } else {
                        Log.i("PowerSaveModeDetector", "Power save mode OFF")
                        // Device exited power save mode
                        isPowerSaveMode = false
                    }
                }

                Intent.ACTION_BATTERY_LOW -> {
                    isPowerSaveMode = true
                    Log.i("PowerSaveModeDetector", "Battery low")
                    // Battery is low
                }

                Intent.ACTION_POWER_CONNECTED -> {
                    Log.i("PowerSaveModeDetector", "Power connected")
                    isPowerSaveMode = false
                    // Power is connected
                }

            }
        }
    }


        fun register() {
            val intentFilter = IntentFilter().apply {
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_POWER_CONNECTED)
            }
            context.registerReceiver(powerSaveModeReceiver, intentFilter)
        }

        fun unregister() {
            context.unregisterReceiver(powerSaveModeReceiver)
        }

}
