package com.aricneto.twistytimer.ui.screens

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aricneto.twistytimer.ui.components.CubeComponent
import com.aricneto.twistytimer.ui.components.QAButtons
import com.aricneto.twistytimer.ui.components.ScrambleBox
import com.aricneto.twistytimer.ui.components.StatsBar
import com.aricneto.twistytimer.ui.components.TimerDisplay
import com.aricneto.twistytimer.ui.components.TimerStats

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
    scrambleState: String? = null,
    scrambleDrawable: Drawable? = null,
    stats: TimerStats? = null,
    showQAButtons: Boolean = false,
    isPersonalBest: Boolean = false,
    onScrambleReset: () -> Unit = {},
    onScrambleEdit: () -> Unit = {},
    onScrambleHintClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onDnfClick: () -> Unit = {},
    onPlusTwoClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    manualEntryEnabled: Boolean = false,
    onManualEntryClick: () -> Unit = {}
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
                showManualEntry = manualEntryEnabled,
                onResetClick = onScrambleReset,
                onEditClick = onScrambleEdit,
                onHintClick = onScrambleHintClick,
                onManualEntryClick = onManualEntryClick
            )

            if (isPersonalBest) {
                Text(
                    text = "Personal Best!",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TimerDisplay(
                timeMillis = currentTimeMillis,
                color = if (isReady) Color.Green else if (isRunning) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
            )

            if (showQAButtons) {
                QAButtons(
                    onRemoveClick = onRemoveClick,
                    onDnfClick = onDnfClick,
                    onPlusTwoClick = onPlusTwoClick,
                    onCommentClick = onCommentClick
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Scramble image below timer
            if (!isRunning) {
                Box(
                    modifier = Modifier
                        .size(width = 240.dp, height = 180.dp)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (scrambleState != null) {
                        CubeComponent(
                            state = scrambleState,
                            modifier = Modifier.size(120.dp)
                        )
                    } else if (scrambleDrawable != null) {
                        AndroidView(
                            factory = { context ->
                                ImageView(context).apply {
                                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                    setImageDrawable(scrambleDrawable)
                                }
                            },
                            update = { it.setImageDrawable(scrambleDrawable) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (stats != null) {
                StatsBar(stats = stats)
            }
        }
    }
}
