package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.ui.components.AlgItem
import com.aricneto.twistytimer.ui.components.TwistyTopBar

@Composable
fun AlgListScreen(
    algorithms: List<Algorithm>,
    subsetName: String?,
    onItemClick: (Algorithm) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TwistyTopBar(
                title = "Algorithms",
                subtitle = subsetName,
                onSettingsClick = onSettingsClick
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(6.dp),
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            items(algorithms) { algorithm ->
                AlgItem(
                    algorithm = algorithm,
                    onClick = { onItemClick(algorithm) }
                )
            }
        }
    }
}
