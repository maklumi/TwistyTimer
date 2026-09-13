package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aricneto.twistytimer.ui.components.ScrambleBox
import com.aricneto.twistytimer.ui.components.TimerDisplay

@Composable
fun TimerScreen(
    scramble: String,
    currentTimeMillis: Long,
    isRunning: Boolean,
    isReady: Boolean,
    onTimerDown: () -> Unit,
    onTimerUp: () -> Unit,
    modifier: Modifier = Modifier,
    isScrambleLoading: Boolean = false,
    showScrambleHint: Boolean = false,
    onScrambleReset: () -> Unit = {},
    onScrambleEdit: () -> Unit = {},
    onScrambleHintClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onTimerDown()
                        tryAwaitRelease()
                        onTimerUp()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScrambleBox(
                scramble = scramble,
                isLoading = isScrambleLoading,
                showHint = showScrambleHint,
                onResetClick = onScrambleReset,
                onEditClick = onScrambleEdit,
                onHintClick = onScrambleHintClick
            )

            Spacer(modifier = Modifier.weight(1f))

            TimerDisplay(
                timeMillis = currentTimeMillis,
                color = if (isReady) Color.Green else if (isRunning) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
