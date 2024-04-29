package it.dii.unipi.masss.noisemapper
import com.google.gson.Gson
import java.io.File

data class BeaconData(val room: String, val beacon: String)
class BLEConfig(noiseDetection: NoiseDetection) {
    fun readJsonFile(filePath: String): List<BeaconData> {
        val jsonString = File(filePath).readText()
        val gson = Gson()
        val beaconDataArray = gson.fromJson(jsonString, Array<BeaconData>::class.java)
        return beaconDataArray.toList()
    }

    fun getBeaconData(): List<BeaconData> {
        val beaconDataList = readJsonFile("data.json")

        for (beaconData in beaconDataList) {
            println("Room: ${beaconData.room} - Beacon: ${beaconData.beacon}")
        }
        return beaconDataList
    }
}
