package com.aricneto.twistytimer.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aricneto.twistytimer.stats.ChartStyle
import com.aricneto.twistytimer.ui.components.LineChartComponent
import com.aricneto.twistytimer.ui.components.StatsDetailTables
import com.aricneto.twistytimer.viewmodel.TimerGraphViewModel

@Composable
fun StatsGraphScreen(
    currentPuzzle: String?,
    currentSubtype: String?,
    mode: Int,
    isForCurrentSessionOnly: Boolean,
    modifier: Modifier = Modifier
) {
    val viewModel: TimerGraphViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(currentPuzzle, currentSubtype, mode, isForCurrentSessionOnly) {
        viewModel.setChartStyle(ChartStyle(context as Activity))
        viewModel.updateParams(currentPuzzle, currentSubtype, isForCurrentSessionOnly, mode)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        var currentSessionOnly by remember { mutableStateOf(isForCurrentSessionOnly) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Session Only", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = currentSessionOnly,
                onCheckedChange = { 
                    currentSessionOnly = it
                    viewModel.updateParams(currentPuzzle, currentSubtype, it, mode)
                }
            )
        }

        LineChartComponent(
            allTimes = uiState.allTimes,
            bestTimes = uiState.bestTimes,
            averages = uiState.averages
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.statistics != null) {
            StatsDetailTables(stats = uiState.statistics!!)
        }
    }
}
