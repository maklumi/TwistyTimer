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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aricneto.twistytimer.ui.components.CubeComponent
import com.aricneto.twistytimer.ui.components.QAButtons
import com.aricneto.twistytimer.ui.components.ScrambleBox
import com.aricneto.twistytimer.ui.components.StatsBar
import com.aricneto.twistytimer.ui.components.TimerDisplay
import com.aricneto.twistytimer.ui.components.TimerStats

data class TimerUiState(
    val scramble: String = "",
    val currentTimeMillis: Long = 0L,
    val isRunning: Boolean = false,
    val isReady: Boolean = false,
    val isScrambleLoading: Boolean = false,
    val showScrambleHint: Boolean = false,
    val scrambleState: String? = null,
    val scrambleDrawable: Drawable? = null,
    val stats: TimerStats? = null,
    val showQAButtons: Boolean = false,
    val isPersonalBest: Boolean = false,
    val manualEntryEnabled: Boolean = false,
    val scrambleTextSize: Int = 100,
    val timerTextSize: Int = 100,
    val hideTimeWhileRunning: Boolean = false,
    val showMillis: Boolean = true
)

data class TimerActions(
    val onTimerDown: () -> Unit = {},
    val onTimerUp: () -> Unit = {},
    val onScrambleReset: () -> Unit = {},
    val onScrambleEdit: () -> Unit = {},
    val onScrambleHintClick: () -> Unit = {},
    val onRemoveClick: () -> Unit = {},
    val onDnfClick: () -> Unit = {},
    val onPlusTwoClick: () -> Unit = {},
    val onCommentClick: () -> Unit = {},
    val onManualEntryClick: () -> Unit = {}
)

@Composable
fun TimerScreen(
    uiState: TimerUiState,
    modifier: Modifier = Modifier,
    actions: TimerActions = TimerActions()
) {
    val currentActions by rememberUpdatedState(actions)

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (uiState.isRunning) Modifier.safeDrawingPadding() else Modifier)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        currentActions.onTimerDown()
                        tryAwaitRelease()
                        currentActions.onTimerUp()
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isRunning) {
                ScrambleBox(
                    scramble = uiState.scramble,
                    isLoading = uiState.isScrambleLoading,
                    showHint = uiState.showScrambleHint,
                    showManualEntry = uiState.manualEntryEnabled,
                    onResetClick = currentActions.onScrambleReset,
                    onEditClick = currentActions.onScrambleEdit,
                    onHintClick = currentActions.onScrambleHintClick,
                    onManualEntryClick = currentActions.onManualEntryClick,
                    fontSize = (18 * (uiState.scrambleTextSize / 100f)).sp
                )

                if (uiState.isPersonalBest) {
                    Text(
                        text = "Personal Best!",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TimerDisplay(
                timeMillis = uiState.currentTimeMillis,
                color = if (uiState.isReady) Color.Green else if (uiState.isRunning) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary,
                fontSize = (76 * (uiState.timerTextSize / 100f)).sp,
                showMillis = uiState.showMillis,
                isSolvingHidden = uiState.isRunning && uiState.hideTimeWhileRunning
            )

            if (uiState.showQAButtons) {
                QAButtons(
                    onRemoveClick = currentActions.onRemoveClick,
                    onDnfClick = currentActions.onDnfClick,
                    onPlusTwoClick = currentActions.onPlusTwoClick,
                    onCommentClick = currentActions.onCommentClick
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!uiState.isRunning) {
                // Scramble image below timer
                Box(
                    modifier = Modifier
                        .size(width = 240.dp, height = 180.dp)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.scrambleState != null) {
                        CubeComponent(
                            state = uiState.scrambleState,
                            modifier = Modifier.size(120.dp)
                        )
                    } else if (uiState.scrambleDrawable != null) {
                        AndroidView(
                            factory = { context ->
                                ImageView(context).apply {
                                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                    setImageDrawable(uiState.scrambleDrawable)
                                }
                            },
                            update = { it.setImageDrawable(uiState.scrambleDrawable) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (uiState.stats != null) {
                    StatsBar(stats = uiState.stats)
                }
            }
        }
    }
}
