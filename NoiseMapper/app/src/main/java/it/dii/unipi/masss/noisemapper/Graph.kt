package it.dii.unipi.masss.noisemapper

import android.content.Context
import org.jetbrains.letsPlot.export.ggsave
import org.jetbrains.letsPlot.geom.geomDensity
import org.jetbrains.letsPlot.ggsize
import org.jetbrains.letsPlot.letsPlot

class Graph {
    fun makeplot(context: Context) {
        val rand = java.util.Random()
        val data = mapOf(
            "rating" to List(200) { rand.nextGaussian() } + List(200) { rand.nextGaussian() * 1.5 + 1.5 },
            "cond" to List(200) { "A" } + List(200) { "B" }
        )

        var p = letsPlot(data)
        p += geomDensity(color = "dark_green", alpha = .3) { x = "rating"; fill = "cond" }
        p + ggsize(700, 350)
        ggsave(p, filename="output.svg", path=context.filesDir.absolutePath)
    }
}
