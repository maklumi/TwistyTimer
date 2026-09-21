package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.ui.components.AlgItem
import com.aricneto.twistytimer.ui.components.TwistyTopBar
import com.aricneto.twistytimer.ui.theme.LocalTwistyColors

@Composable
fun AlgListScreen(
    algorithms: List<Algorithm>,
    subsetName: String?,
    onItemClick: (Algorithm) -> Unit,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TwistyTopBar(
                title = "Algorithms",
                subtitle = subsetName,
                onBackClick = onBackClick,
                onSettingsClick = onSettingsClick
            )
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
                AlgItem(
                    algorithm = algorithm,
                    onClick = { onItemClick(algorithm) }
                )
            }
        }
    }
}
