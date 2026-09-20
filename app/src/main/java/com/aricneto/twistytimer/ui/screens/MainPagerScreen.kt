package com.aricneto.twistytimer.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.aricneto.twistify.R
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.ui.components.TimerStats
import com.aricneto.twistytimer.ui.components.TwistyTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPagerScreen(
    currentPuzzle: String,
    currentCategory: String,
    mode: Int,
    scramble: String,
    currentTimeMillis: Long,
    isRunning: Boolean,
    isReady: Boolean,
    isScrambleLoading: Boolean,
    showScrambleHint: Boolean,
    showQAButtons: Boolean,
    scrambleDrawable: Drawable?,
    solves: List<Solve>,
    stats: TimerStats?,
    selectedSolveIds: Set<Long>,
    onTimerDown: () -> Unit,
    onTimerUp: () -> Unit,
    onSettingsClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onTitleClick: () -> Unit = {},
    onSolveClick: (Solve) -> Unit,
    onSolveLongClick: (Solve) -> Unit = {},
    onSearchChange: (String) -> Unit = {},
    onSortClick: () -> Unit = {},
    showClearButton: Boolean = false,
    onClearClick: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onScrambleReset: () -> Unit = {},
    onScrambleEdit: () -> Unit = {},
    onScrambleHintClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onDnfClick: () -> Unit = {},
    onPlusTwoClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    manualEntryEnabled: Boolean = false,
    onManualEntryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    
    Scaffold(
        topBar = {
            if (!isRunning) {
                if (selectedSolveIds.isNotEmpty()) {
                    TopAppBar(
                        title = { Text("${selectedSolveIds.size} Selected") },
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
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_timeline_24px), contentDescription = null) },
                        label = { Text("Stats") }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_timer_24px), contentDescription = null) },
                        label = { Text("Timer") }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 2,
                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                        icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_list_alt_24px), contentDescription = null) },
                        label = { Text("List") }
                    )
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = modifier.padding(paddingValues),
            userScrollEnabled = !isRunning
        ) { page ->
            when (page) {
                0 -> StatsGraphScreen(
                    currentPuzzle = currentPuzzle,
                    currentSubtype = currentCategory,
                    mode = mode,
                    isForCurrentSessionOnly = true // Handle toggle
                )
                1 -> TimerScreen(
                    scramble = scramble,
                    currentTimeMillis = currentTimeMillis,
                    isRunning = isRunning,
                    isReady = isReady,
                    isScrambleLoading = isScrambleLoading,
                    showScrambleHint = showScrambleHint,
                    showQAButtons = showQAButtons,
                    scrambleDrawable = scrambleDrawable,
                    onTimerDown = onTimerDown,
                    onTimerUp = onTimerUp,
                    onScrambleReset = onScrambleReset,
                    onScrambleEdit = onScrambleEdit,
                    onScrambleHintClick = onScrambleHintClick,
                    onRemoveClick = onRemoveClick,
                    onDnfClick = onDnfClick,
                    onPlusTwoClick = onPlusTwoClick,
                    onCommentClick = onCommentClick,
                    manualEntryEnabled = manualEntryEnabled,
                    onManualEntryClick = onManualEntryClick,
                    stats = stats
                )
                2 -> SolvesListScreen(
                    solves = solves,
                    selectedIds = selectedSolveIds,
                    onSolveClick = onSolveClick,
                    onSolveLongClick = onSolveLongClick,
                    onSearchChange = onSearchChange,
                    onSortClick = onSortClick,
                    showClearButton = showClearButton,
                    onClearClick = onClearClick
                )
            }
        }
    }
}
