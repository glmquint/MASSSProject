package it.dii.unipi.masss.noisemapper

import android.content.Context
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomBar
import org.jetbrains.letsPlot.geom.geomBoxplot
import org.jetbrains.letsPlot.geom.geomLabel
import org.jetbrains.letsPlot.geom.geomPolygon
import org.jetbrains.letsPlot.geom.geomRect
import org.jetbrains.letsPlot.geom.geomText
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleFillGradient

class Graph (private val context: Context){
    fun makeplot() {
        val rooms = mapOf(
            "room_id" to listOf("room1", "room2", "room3"),
            "x1" to listOf(1.0, 2.0, 3.0),
            "x2" to listOf(4.0, 5.0, 6.0),
            "y1" to listOf(7.0, 8.0, 9.0),
            "y2" to listOf(10.0, 11.0, 12.0),
            "noise_level" to listOf(1000.0, 20.0, 256.0),
            "color" to listOf("red", "green", "blue")
        )
        var p = letsPlot(data = rooms)
        p += geomRect(alpha = 0.8) {
            xmin = "x1"
            xmax = "x2"
            ymin = "y1"
            ymax = "y2"
            fill = "noise_level"
        }
        p += geomText(nudgeY = 0.5){
            x = "x1"
            y = "y1"
            label = "room_id"
        }
        p += ggtitle("Noise levels of different rooms")
        p += scaleFillGradient(low = "green", high = "red")
        p += ggsize(700, 350)
        ggsave(p, filename="output.svg", path=context.filesDir.absolutePath)
    }
}
