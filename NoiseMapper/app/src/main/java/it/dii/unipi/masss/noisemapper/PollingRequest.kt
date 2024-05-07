package it.dii.unipi.masss.noisemapper

import android.content.Context
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class PollingRequest(private val context: Context) {
    private val url = context.getString(R.string.serverURL) + "/measurements"
    private val interval = 10000L
    private var timer: Timer? = null

    fun start() {
        timer = fixedRateTimer(initialDelay = 2000, period = interval) {
            // Get current time
            val currentTime = Calendar.getInstance()

            // Get time an hour before
            val timeAnHourBefore = Calendar.getInstance()
            timeAnHourBefore.add(Calendar.HOUR_OF_DAY, -1)

            // Convert to Unix timestamp
            val currentUnixTimestamp = currentTime.timeInMillis / 1000
            val previousHourUnixTimestamp = timeAnHourBefore.timeInMillis / 1000

            // Log the results
            Log.i("PollingRequest", "Current time: ${currentTime.time}")
            Log.i("PollingRequest", "Unix timestamp of current time: $currentUnixTimestamp")

            Log.i("PollingRequest", "Time an hour before: ${timeAnHourBefore.time}")
            Log.i("PollingRequest", "Unix timestamp of time an hour before: $previousHourUnixTimestamp")
            performGetRequest(previousHourUnixTimestamp,currentUnixTimestamp)
        }
    }

    // Stop the timer
    fun stop() {
        timer?.cancel()
        timer = null
    }

     fun performGetRequest(start_from:Long, end_to:Long) {
         Thread {
             try {
                 val url = URL("$url?start_from=$start_from&end_to=$end_to")
                 val connection = url.openConnection() as HttpURLConnection
                 connection.requestMethod = "GET"

                 val responseCode = connection.responseCode
                 if (responseCode == HttpURLConnection.HTTP_OK) {
                     val response = connection.inputStream.bufferedReader().readText()
                     Log.i("PollingRequest", "GET request successful with response: $response")
                 } else {
                     Log.e("PollingRequest", "GET request failed with response code: $responseCode")
                 }

                 connection.disconnect()
             } catch (e: Exception) {
                 Log.e("PollingRequest", "GET request failed with exception: $e")
             }
         }.start()
    }
}


