package it.dii.unipi.masss.noisemapper

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader

interface FileDownloadCallback {

    fun onFileDownloaded(filePath: String)
    fun onFileDownloadError(errorMessage: String)
}

class ConfigData(val mapping : Map<String, String>, val layout : Map<String, List<Any>>)

class BLEConfig(private val context: Context, offline: Boolean = false) {
    private val successfulConfig: Boolean
    lateinit var beaconRoomMap : ConfigData
    val url = context.getString(R.string.serverURL) + "/resources/"+ context.getString(R.string.config_file_name)
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

            synchronized(lock) {
                val jsonString = readFromFile(context, context.getString(R.string.config_file_name))
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
                Log.d("NoiseMapper","Config file correctly downloaded")
            }

            override fun onFileDownloadError(errorMessage: String) {
                Log.e("NoiseMapper", "Error on downloading BLE config $errorMessage")
            }
        }
        val noise_map_io : NoiseMapIO = NoiseMapIO(context);
        noise_map_io.retrieveFileFromServer(callback , fileToSave="config.json",lock)
    }



    fun gotConfig(): Boolean {
        return successfulConfig
    }
}
// "the value is saved /data/user/0/it.dii.unipi.masss.noisemapper/files/config.json"