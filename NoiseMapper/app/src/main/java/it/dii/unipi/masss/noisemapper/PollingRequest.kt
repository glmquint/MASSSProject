package it.dii.unipi.masss.noisemapper

import android.content.Context
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.fixedRateTimer

class PollingRequest(private val context: Context) {
    private val url = context.getString(R.string.serverURL) + "/measurements"
    private val interval = 10000L
    fun start() {
        fixedRateTimer(initialDelay = 2000, period = interval) {
            performGetRequest()
        }
    }

    private fun performGetRequest() {
        try {
            val url = URL(url)
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
    }
}


