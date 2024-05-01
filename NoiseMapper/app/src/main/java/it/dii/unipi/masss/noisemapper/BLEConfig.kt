package it.dii.unipi.masss.noisemapper

import android.content.Context
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.io.*
import java.net.HttpURLConnection
import com.google.gson.JsonObject
import kotlin.system.exitProcess

interface FileDownloadCallback {

    fun onFileDownloaded(filePath: String)
    fun onFileDownloadError(errorMessage: String)
}

data class BeaconData(val room: String, val beacon: String)
class BLEConfig(noiseDetection: NoiseDetection, private val context: Context) {
    var lock = Object()

    fun readJsonFile(filePath: String): List<BeaconData> {

        val jsonString = File(filePath).readText()
        val gson = Gson()
        val beaconDataArray = gson.fromJson(jsonString, Array<BeaconData>::class.java)
        return beaconDataArray.toList()
    }

    fun getBeaconData(): List<BeaconData> {

        val beaconDataList = readJsonFile("/data/data.json")

        for (beaconData in beaconDataList) {
            println("Room: ${beaconData.room} - Beacon: ${beaconData.beacon}")
        }
        return beaconDataList
    }
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
    fun readJSONfile(){

        try {
            val gson = Gson()


            val fileName = "config.json"

            synchronized(lock) {
                val jsonString = readFromFile(context, fileName)
                // Parse JSON to JsonObject
                val jsonObject: JsonObject = gson.fromJson(jsonString, JsonObject::class.java)

                // Access JSON elements
                val name = jsonObject["name"].asString
                val age = jsonObject["age"].asInt

                println("Names: $name, Age: $age")
            }
        }
        catch (e: Exception) {
            println("errore")
            e.printStackTrace()
        }
    }
    fun prova1() {
            // Create a URL for the desired page


        val context = context // Assuming you have an instance of Context
        val fileName = "example.json"
        val data = "Hello, world!"
        writeToFile(context, fileName, data)
        val fileContents = readFromFile(context, fileName)
        println("File Contents: $fileContents")
    }


    fun retrieveFileFromServer(url: String, context: Context, callback: FileDownloadCallback, fileToSave: String) {

        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                synchronized(lock) {
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onFileDownloadError("Error: ${e.message}")
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

    fun prova(){
        val url = "http://192.168.1.178:8000/config.json"
        GetConfigFileFromServer(url)
        synchronized(lock) {
            lock.wait()
        }
        readJSONfile()
    }
}
// "the value is saved /data/user/0/it.dii.unipi.masss.noisemapper/files/config.json"