package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aricneto.twistify.R
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.puzzle.TrainerScrambler
import com.aricneto.twistytimer.ui.components.AlgItem
import com.aricneto.twistytimer.ui.components.TwistyTopBar
import com.aricneto.twistytimer.ui.theme.LocalTwistyColors

@Composable
fun TrainerSubsetScreen(
    subsetName: String,
    categoryName: String,
    algorithms: List<Algorithm>,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onItemClick: (Algorithm) -> Unit,
    modifier: Modifier = Modifier
) {
    val subset = remember(subsetName) { TrainerScrambler.TrainerSubset.valueOf(subsetName) }
    
    val selectedItems = remember(subsetName, categoryName) {
        TrainerScrambler.fetchSelectedItems(subset, categoryName)?.toMutableStateSet() ?: mutableStateSetOf<String>()
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TwistyTopBar(
                title = stringResource(R.string.trainer_spinner_title),
                subtitle = subsetName,
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = Color.Transparent) {
                Button(
                    onClick = {
                        val allNames = algorithms.map { it.name }
                        selectedItems.clear()
                        selectedItems.addAll(allNames)
                        TrainerScrambler.saveSelectedItems(subset, categoryName, selectedItems.toMutableList())
                        onBackClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.trainer_select_all))
                }
            }
        },
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    LocalTwistyColors.current.backgroundGradientStart,
                    LocalTwistyColors.current.backgroundGradientEnd
                )
            )
        )
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(6.dp),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            items(algorithms) { algorithm ->
                val isSelected = selectedItems.contains(algorithm.name)
                Box(modifier = Modifier.padding(2.dp)) {
                    AlgItem(
                        algorithm = algorithm,
                        onClick = {
                            if (isSelected) {
                                selectedItems.remove(algorithm.name)
                            } else {
                                selectedItems.add(algorithm.name)
                            }
                            TrainerScrambler.saveSelectedItems(subset, categoryName, selectedItems.toMutableList())
                        },
                        onLongClick = { onItemClick(algorithm) },
                        modifier = Modifier.padding(4.dp)
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null, // Handled by AlgItem click
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

private fun <T> Set<T>.toMutableStateSet(): SnapshotStateSet<T> {
    val set = mutableStateSetOf<T>()
    set.addAll(this)
    return set
}
