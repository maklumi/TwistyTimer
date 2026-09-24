package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        data.add(
            Line(
                label = "Best",
                values = bestTimes.map { it.second.toDouble() },
                color = SolidColor(Color(0xFFFF5252)),
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
                        5 -> Color(0xFF4CAF50)
                        12 -> Color(0xFFFFEB3B)
                        50 -> Color(0xFFE91E63)
                        100 -> Color(0xFF00BCD4)
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
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
                labelCountPerLine = 4
            ),
            modifier = modifier.fillMaxWidth().height(300.dp)
        )
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.medium
        ) {
            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No solve data available for graph",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
