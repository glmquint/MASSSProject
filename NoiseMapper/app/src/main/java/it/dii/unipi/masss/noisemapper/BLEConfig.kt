package it.dii.unipi.masss.noisemapper

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.io.*
import java.net.HttpURLConnection

interface FileDownloadCallback {

    fun onFileDownloaded(filePath: String)
    fun onFileDownloadError(errorMessage: String)
}

class BLEConfig(private val context: Context) {
    var beaconRoomMap = mutableMapOf<String, String>()
    val url = context.getString(R.string.serverURL) + "/resources/config.json"
    var lock = Object()

    fun writeToFile(context: Context, fileName: String, data: String) {
        try {
            val fileOutputStream: FileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            fileOutputStream.write(data.toByteArray())
            fileOutputStream.close()
            println("Data has been written to the file.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun readFromFile(context: Context, fileName: String): String {
        val stringBuilder = StringBuilder()
        try {
            val fileInputStream = context.openFileInput(fileName)
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var text: String? = bufferedReader.readLine()
            while (text != null) {
                stringBuilder.append(text).append("\n")
                text = bufferedReader.readLine()
            }
            fileInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stringBuilder.toString()
    }
    fun readJSONfile(): Boolean {

        try {
            val gson = Gson()


            val fileName = "config.json"

            synchronized(lock) {
                val jsonString = readFromFile(context, fileName)
                // Parse JSON to JsonObject
                beaconRoomMap = gson.fromJson(jsonString, mutableMapOf<String, String>()::class.java)

                for ((key, value) in beaconRoomMap) {
                    println("Beacon: $key, Room: $value")
                }
            }
        }
        catch (e: Exception) {
            println("errore")
            e.printStackTrace()
            Toast.makeText(context, "Error: Unable to retrieve configuration file. Please check you internet connection", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }


    fun retrieveFileFromServer(url: String, context: Context, callback: FileDownloadCallback, fileToSave: String) {

        Thread {
            synchronized(lock) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
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
                } catch (e: Exception) {
                    e.printStackTrace()
                    callback.onFileDownloadError("Error: ${e.message}")
                }
                lock.notify()
            }
        }.start()
    }

    // Usage
    fun GetConfigFileFromServer(url: String) {
        //type the url of the server

        val callback = object : FileDownloadCallback {
            override fun onFileDownloaded(filePath: String) {
                println("File downloaded: $filePath")
            }

            override fun onFileDownloadError(errorMessage: String) {
                println(errorMessage)
            }
        }

        retrieveFileFromServer(url, context, callback , fileToSave="config.json")
        //val fileName = "data.json"
        //val fileContents = readFromFile(context, fileName)
        //println("File Contents: $fileContents")
    }

    fun getConfig(offline: Boolean = false): Boolean {
        if (!offline) {
            GetConfigFileFromServer(url)
            synchronized(lock) {
                lock.wait()
            }
        }
        return readJSONfile()
    }
}
// "the value is saved /data/user/0/it.dii.unipi.masss.noisemapper/files/config.json"