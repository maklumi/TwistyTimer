package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.Line

@Composable
fun LineChartComponent(
    allTimes: List<Pair<Float, Float>>,
    bestTimes: List<Pair<Float, Float>>,
    averages: Map<Int, List<Pair<Float, Float>>>,
    modifier: Modifier = Modifier
) {
    val data = mutableListOf<Line>()

    // All Times
    if (allTimes.isNotEmpty()) {
        data.add(
            Line(
                label = "Solves",
                values = allTimes.map { it.second.toDouble() },
                color = SolidColor(Color(0xFF00ADB5)),
                firstGradientFillColor = Color(0xFF00ADB5).copy(alpha = 0.3f),
                secondGradientFillColor = Color.Transparent,
                curvedEdges = false
            )
        )
    }

    // Best Times
    if (bestTimes.isNotEmpty()) {
        // Best times in legacy were a staircase. 
        // We might need to interpolate if the library just draws points.
        // For now, let's just pass the values.
        data.add(
            Line(
                label = "Best",
                values = bestTimes.map { it.second.toDouble() },
                color = SolidColor(Color.Red),
                curvedEdges = false
            )
        )
    }

    // Averages
    averages.forEach { (n, points) ->
        if (points.isNotEmpty()) {
            data.add(
                Line(
                    label = "Ao$n",
                    values = points.map { it.second.toDouble() },
                    color = SolidColor(when(n) {
                        5 -> Color.Green
                        12 -> Color.Yellow
                        50 -> Color.Magenta
                        100 -> Color.Cyan
                        else -> Color.Gray
                    }),
                    curvedEdges = true
                )
            )
        }
    }

    if (data.isNotEmpty()) {
        LineChart(
            data = data,
            labelHelperProperties = LabelHelperProperties(
                enabled = true,
                textStyle = TextStyle(color = Color.Gray),
                labelCountPerLine = 4
            ),
            modifier = modifier.fillMaxWidth().height(300.dp)
        )
    }
}
