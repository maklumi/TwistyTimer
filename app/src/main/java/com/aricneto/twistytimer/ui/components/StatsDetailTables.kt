package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aricneto.twistify.R
import com.aricneto.twistytimer.stats.AverageCalculator.Companion.tr
import com.aricneto.twistytimer.stats.Statistics
import com.aricneto.twistytimer.utils.PuzzleUtils

@Composable
fun StatsDetailTables(
    stats: Statistics,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Improvement", "Average", "Other")

    Column(modifier = modifier.fillMaxWidth()) {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> ImprovementTable(stats)
            1 -> AverageTable(stats)
            2 -> OtherTable(stats)
        }
    }
}

@Composable
private fun ImprovementTable(stats: Statistics) {
    val labels = stringArrayResource(R.array.stats_column_improvement)
    val columns = listOf("Global Best", "Session Best")
    
    val values = remember(stats) {
        val list = mutableListOf<String>()
        // Dev
        list.add(fmt(stats.allTimeStdDeviation))
        list.add(fmt(stats.sessionStdDeviation))
        // Ao12
        list.add(fmt(stats.getAverageOf(12, false)?.bestAverage ?: -666L))
        list.add(fmt(stats.getAverageOf(12, true)?.bestAverage ?: -666L))
        // Ao50
        list.add(fmt(stats.getAverageOf(50, false)?.bestAverage ?: -666L))
        list.add(fmt(stats.getAverageOf(50, true)?.bestAverage ?: -666L))
        // Ao100
        list.add(fmt(stats.getAverageOf(100, false)?.bestAverage ?: -666L))
        list.add(fmt(stats.getAverageOf(100, true)?.bestAverage ?: -666L))
        // Best
        list.add(fmt(stats.allTimeBestTime))
        list.add(fmt(stats.sessionBestTime))
        // Count
        list.add(stats.allTimeNumSolves.toString())
        list.add(stats.sessionNumSolves.toString())
        list
    }

    BaseStatsGrid(labels = labels, columnTitles = columns, values = values)
}

@Composable
private fun AverageTable(stats: Statistics) {
    val labels = stringArrayResource(R.array.stats_column_average)
    val columns = listOf("Global Best", "Session Best", "Current")
    val averageNumbers = intArrayOf(3, 5, 12, 50, 100, 1000)

    val values = remember(stats) {
        val list = mutableListOf<String>()
        for (n in averageNumbers) {
            list.add(fmt(stats.getAverageOf(n, false)?.bestAverage ?: -666L))
            list.add(fmt(stats.getAverageOf(n, true)?.bestAverage ?: -666L))
            list.add(fmt(stats.getAverageOf(n, true)?.currentAverage ?: -666L))
        }
        list
    }

    BaseStatsGrid(labels = labels, columnTitles = columns, values = values)
}

@Composable
private fun OtherTable(stats: Statistics) {
    val labels = stringArrayResource(R.array.stats_column_other)
    val columns = listOf("Global", "Session")

    val values = remember(stats) {
        val list = mutableListOf<String>()
        // Best
        list.add(fmt(stats.allTimeBestTime))
        list.add(fmt(stats.sessionBestTime))
        // Worst
        list.add(fmt(stats.allTimeWorstTime))
        list.add(fmt(stats.sessionWorstTime))
        // Dev
        list.add(fmt(stats.allTimeStdDeviation))
        list.add(fmt(stats.sessionStdDeviation))
        // Mean
        list.add(fmt(stats.allTimeMeanTime))
        list.add(fmt(stats.sessionMeanTime))
        // Total
        list.add(fmt(stats.allTimeTotalTime, true))
        list.add(fmt(stats.sessionTotalTime, true))
        // Count
        list.add(stats.allTimeNumSolves.toString())
        list.add(stats.sessionNumSolves.toString())
        list
    }

    BaseStatsGrid(labels = labels, columnTitles = columns, values = values)
}

@Composable
private fun BaseStatsGrid(
    labels: Array<String>,
    columnTitles: List<String>,
    values: List<String>
) {
    val numCols = columnTitles.size
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
            Box(modifier = Modifier.weight(2f).padding(8.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_sigma),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).align(Alignment.Center)
                )
            }
            columnTitles.forEach { title ->
                Text(
                    text = title,
                    modifier = Modifier.weight(3f).padding(8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Rows
        labels.forEachIndexed { rowIndex, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (rowIndex % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(2f).padding(8.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                for (i in 0 until numCols) {
                    val value = values.getOrNull(rowIndex * numCols + i) ?: "--"
                    Text(
                        text = value,
                        modifier = Modifier.weight(3f).padding(8.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun fmt(time: Long, large: Boolean = false): String {
    return PuzzleUtils.convertTimeToString(
        tr(time),
        if (large) PuzzleUtils.FORMAT_LARGE else PuzzleUtils.FORMAT_DEFAULT
    )
}
