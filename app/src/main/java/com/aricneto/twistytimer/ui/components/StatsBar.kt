package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TimerStats(
    val deviation: String = "--",
    val mean: String = "--",
    val best: String = "--",
    val count: String = "0",
    val ao5: String = "--",
    val ao12: String = "--",
    val ao50: String = "--",
    val ao100: String = "--",
    val isAo5Record: Boolean = false,
    val isAo12Record: Boolean = false,
    val isAo50Record: Boolean = false,
    val isAo100Record: Boolean = false
)

@Composable
fun StatsBar(
    stats: TimerStats,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left Column: Other Stats (Dev, Mean, Best, Count)
        Column(horizontalAlignment = Alignment.Start) {
            StatText("Dev: ${stats.deviation}", color)
            StatText("Mean: ${stats.mean}", color)
            StatText("Best: ${stats.best}", color)
            StatText("Count: ${stats.count}", color)
        }

        // Right Column: Averages (Ao5, Ao12, Ao50, Ao100)
        Column(horizontalAlignment = Alignment.End) {
            AvgStatText("Ao5", stats.ao5, stats.isAo5Record, color)
            AvgStatText("Ao12", stats.ao12, stats.isAo12Record, color)
            AvgStatText("Ao50", stats.ao50, stats.isAo50Record, color)
            AvgStatText("Ao100", stats.ao100, stats.isAo100Record, color)
        }
    }
}

@Composable
private fun StatText(text: String, color: Color) {
    Text(
        text = text,
        color = color.copy(alpha = 0.85f),
        fontSize = 12.sp,
        textAlign = TextAlign.Left,
        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
    )
}

@Composable
private fun AvgStatText(label: String, value: String, isRecord: Boolean, color: Color) {
    val text = buildAnnotatedString {
        append("$label: ")
        if (isRecord) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
            append(value)
            pop()
        } else {
            append(value)
        }
    }
    Text(
        text = text,
        color = color.copy(alpha = 0.85f),
        fontSize = 12.sp,
        textAlign = TextAlign.Right,
        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
    )
}
