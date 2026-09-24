package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aricneto.twistify.R
import com.aricneto.twistytimer.ui.components.TwistyTopBar
import com.aricneto.twistytimer.ui.theme.LocalTwistyColors
import com.aricneto.twistytimer.utils.Prefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPagerScreen(
    currentPuzzle: String,
    currentCategory: String,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    swipingEnabled: Boolean = Prefs.getBoolean(R.string.pk_tab_swiping_enabled, true),
    selectedSolveCount: Int = 0,
    onSettingsClick: () -> Unit = {},
    onCategoryClick: () -> Unit = {},
    onTitleClick: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    statsPage: @Composable () -> Unit,
    timerPage: @Composable () -> Unit,
    solvesListPage: @Composable () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isRunning) {
                if (selectedSolveCount > 0) {
                    TopAppBar(
                        title = { Text("$selectedSolveCount Selected") },
                        navigationIcon = {
                            IconButton(onClick = onClearSelection) {
                                Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_clear_black_24dp), contentDescription = "Clear")
                            }
                        },
                        actions = {
                            IconButton(onClick = onDeleteSelected) {
                                Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_delete_24dp), contentDescription = "Delete")
                            }
                        }
                    )
                } else {
                    TwistyTopBar(
                        title = currentPuzzle,
                        subtitle = currentCategory,
                        onSettingsClick = onSettingsClick,
                        onCategoryClick = onCategoryClick,
                        onTitleClick = onTitleClick,
                        showSpinnerIcon = true
                    )
                }
            }
        },
        bottomBar = {
            if (!isRunning) {
                val scope = rememberCoroutineScope()
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_timeline_24px), contentDescription = null) },
                        label = {
                            Text(
                                text = "Stats",
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_timer_24px), contentDescription = null) },
                        label = {
                            Text(
                                text = "Timer",
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 2,
                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                        icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_list_alt_24px), contentDescription = null) },
                        label = {
                            Text(
                                text = "List",
                                fontWeight = if (pagerState.currentPage == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = navItemColors
                    )
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(paddingValues),
            userScrollEnabled = !isRunning && swipingEnabled
        ) { page ->
            when (page) {
                0 -> statsPage()
                1 -> timerPage()
                2 -> solvesListPage()
            }
        }
    }
}
