package it.dii.unipi.masss.noisemapper

import android.util.Log
import org.jetbrains.letsPlot.core.spec.back.transform.bistro.util.theme
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomRect
import org.jetbrains.letsPlot.geom.geomText
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleFillGradient

class Graph(private val filesDir: String, private val bleConfig: BLEConfig){
    fun makeplot(room_noise: Map<String, Double>, filename:String) {
        val room_mapping = bleConfig.beaconRoomMap.layout

        val noiseLevels = room_mapping?.get("room_name")?.map { room_noise[it] }
        val noiseLabels = room_mapping?.get("room_name")?.map { "$it: \n${String.format("%.2f", room_noise[it]?:0.0)} dB" }
        val updatedRoomMapping = room_mapping?.plus(mapOf("noise_level" to noiseLevels, "noise_labels" to noiseLabels))
        var p = letsPlot(data = updatedRoomMapping)
        p += geomRect(alpha = 0.8) {
            xmin = "x1"
            xmax = "x2"
            ymin = "y1"
            ymax = "y2"
            fill = "noise_level"
        }
        p += geomText(nudgeY = 1, hjust = 0, vjust=0){
            x = "x1"
            y = "y1"
            label = "noise_labels"
        }
        p += ggtitle("Noise levels of different rooms")
        p += scaleFillGradient(low = "green", high = "red")
        ggsave(p, filename=filename, path=filesDir)
    }
}
