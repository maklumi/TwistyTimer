package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun TimerDisplay(
    timeMillis: Long,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 76.sp,
    color: Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight = FontWeight.Bold,
    showMillis: Boolean = true,
    smallMillis: Boolean = true,
    isSolvingHidden: Boolean = false
) {
    val annotatedTime = if (isSolvingHidden) {
        AnnotatedString("...")
    } else if (timeMillis < 0) {
        AnnotatedString("DNF")
    } else if (timeMillis == 0L) {
        AnnotatedString("--")
    } else {
        buildAnnotatedString {
            val secondsTotal = timeMillis / 1000
            val millis = (timeMillis % 1000) / 10
            val seconds = secondsTotal % 60
            val minutesTotal = secondsTotal / 60
            val minutes = minutesTotal % 60
            val hours = minutesTotal / 60

            if (hours > 0) {
                append("$hours:")
                append(minutes.toString().padStart(2, '0'))
                append(":")
            } else if (minutes > 0) {
                append("$minutes:")
            }

            if (minutes > 0 || hours > 0) {
                append(seconds.toString().padStart(2, '0'))
            } else {
                append(seconds.toString())
            }

            if (showMillis) {
                val millisStr = millis.toString().padStart(2, '0')
                if (smallMillis) {
                    withStyle(style = SpanStyle(fontSize = (fontSize.value * 0.6).sp)) {
                        append(".")
                        append(millisStr)
                    }
                } else {
                    append(".")
                    append(millisStr)
                }
            }
        }
    }

    Text(
        text = annotatedTime,
        modifier = modifier.fillMaxWidth(),
        fontSize = fontSize,
        color = color,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontFeatureSettings = "tnum"
        )
    )
}
