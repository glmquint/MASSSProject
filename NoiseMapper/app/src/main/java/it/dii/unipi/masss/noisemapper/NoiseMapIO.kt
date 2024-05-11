package it.dii.unipi.masss.noisemapper

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.internal.notify
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

//class that performs the config requests to the server and retrieve the array of tuple (room, noise) sending the time interval
class NoiseMapIO (private val context: Context) {

    private val url = context.getString(R.string.serverURL) + "/measurements"

    fun retrieveFileFromServer(
            context: Context,
            callback: FileDownloadCallback,
            fileToSave: String,
            lock: Any
        ) {

            Thread {
                synchronized(lock) {
                    try {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.connectTimeout = context.resources.getInteger(R.integer.TIMEOUT_CONNECTION)
                        connection.readTimeout = context.resources.getInteger(R.integer.TIMEOUT_CONNECTION)
                        connection.connect()
                        val inputStream = BufferedInputStream(connection.inputStream)
                        val file = File(context.filesDir, fileToSave)
                        println("files_downaload $file")
                        val outputStream = FileOutputStream(file)
                        inputStream.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback.onFileDownloaded(file.absolutePath)
                        inputStream.close()
                        outputStream.close()
                        lock.notify()

                    } catch (e: Exception) {
                        e.printStackTrace()
                        lock.notify()
                        callback.onFileDownloadError("Error: ${e.message}")
                    }
                }
            }.start()
        }

        fun performGetRequest(start_from: Long, end_to: Long): Map<String, Double> {

            var roomNoise = mapOf<String, Double>();
            val lock = Object()
            Thread {
                synchronized(lock) {
                    try {
                        val url = URL("$url?start_from=$start_from&end_to=$end_to")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"

                        val responseCode = connection.responseCode


                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val gson = Gson()
                            val response = gson.fromJson(
                                connection.inputStream.bufferedReader().readText(),
                                Map::class.java
                            )["data"]
                            response as List<Map<String, Any>>

                            //response.groupBy { it["room"] }.map { (room, value) -> room }  // TODO: prepare data to be plotted
                            roomNoise = response.groupBy { it["room"] }.mapValues { (_, samples) ->
                                samples.map { it["noise"]!! as Double }.average()
                            } as Map<String, Double>

                            Log.i(
                                "PollingRequest",
                                "GET request successful with response: $response"
                            )
                        } else {
                            Log.e(
                                "PollingRequest",
                                "GET request failed with response code: $responseCode"

                            )

                        }
                        connection.disconnect()
                        lock.notify() // i release the lock after modifying roomNoise to avoid race condition
                    } catch (e: Exception) {
                        lock.notify() // i release the lock after modifying roomNoise to avoid race condition
                        Log.e("PollingRequest", "GET request failed with exception: $e")
                    }
                }
            }.start()

            synchronized(lock) {
                lock.wait()
            }

            return roomNoise;
        }

}
