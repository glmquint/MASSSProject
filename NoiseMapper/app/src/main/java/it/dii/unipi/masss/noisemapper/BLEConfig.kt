package it.dii.unipi.masss.noisemapper

import android.content.Context
import android.util.Log
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

class ConfigData(val mapping : Map<String, String>, val layout : Map<String, List<Any>>)

class BLEConfig(private val context: Context, offline: Boolean = false) {
    private val successfulConfig: Boolean
    lateinit var beaconRoomMap : ConfigData
    val url = context.getString(R.string.serverURL) + "/resources/config.json"
    var lock = Object()

    init {
        if (!offline) {
                GetConfigFileFromServer(url)
                synchronized(lock) {
                    lock.wait()
                }

        }
        successfulConfig = readJSONfile()
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
                beaconRoomMap = gson.fromJson(jsonString, ConfigData::class.java)
            }
        }
        catch (e: Exception) {
            Log.i("NoiseMapper", "Error reading JSON config file")
            e.printStackTrace()
            return false
        }
        return true
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
        val noise_map_io : NoiseMapIO = NoiseMapIO(context);
        noise_map_io.retrieveFileFromServer(context, callback , fileToSave="config.json",lock)
        //val fileName = "data.json"
        //val fileContents = readFromFile(context, fileName)
        //println("File Contents: $fileContents")
    }



    fun gotConfig(): Boolean {
        return successfulConfig
    }
}
// "the value is saved /data/user/0/it.dii.unipi.masss.noisemapper/files/config.json"