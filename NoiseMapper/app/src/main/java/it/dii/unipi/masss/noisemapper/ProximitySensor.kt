package it.dii.unipi.masss.noisemapper

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ProximitySensorHandler(context: Context) : SensorEventListener {

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    var isNearObject: Boolean = false
    val DB_ADJUSTMENT = context.resources.getInteger(R.integer.DB_ADJUSTMENT)

    fun startListening() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { return }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            isNearObject = it.values[0] < (proximitySensor?.maximumRange ?: 0.0f)
        }
    }

}