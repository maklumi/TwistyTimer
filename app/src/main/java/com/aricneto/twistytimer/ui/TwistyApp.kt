package com.aricneto.twistytimer.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aricneto.twistify.R
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.database.SolveRepository
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.solver.RubiksCubeOptimalCross
import com.aricneto.twistytimer.solver.RubiksCubeOptimalFirstBlock
import com.aricneto.twistytimer.solver.RubiksCubeOptimalSecondBlock
import com.aricneto.twistytimer.solver.RubiksCubeOptimalXCross
import com.aricneto.twistytimer.ui.components.AddTimeComposeDialog
import com.aricneto.twistytimer.ui.components.AlgorithmDetailDialog
import com.aricneto.twistytimer.ui.components.CategorySelectComposeDialog
import com.aricneto.twistytimer.ui.components.HintComposeDialog
import com.aricneto.twistytimer.ui.components.SolveDetailDialog
import com.aricneto.twistytimer.ui.screens.AboutScreen
import com.aricneto.twistytimer.ui.screens.AlgListScreen
import com.aricneto.twistytimer.ui.screens.ColorSchemeScreen
import com.aricneto.twistytimer.ui.screens.ExportImportScreen
import com.aricneto.twistytimer.ui.screens.LanguageSelectScreen
import com.aricneto.twistytimer.ui.screens.MainPagerScreen
import com.aricneto.twistytimer.ui.screens.PuzzleSelectScreen
import com.aricneto.twistytimer.ui.screens.SettingsScreen
import com.aricneto.twistytimer.ui.screens.SolvesListActions
import com.aricneto.twistytimer.ui.screens.SolvesListScreen
import com.aricneto.twistytimer.ui.screens.SolvesListUiState
import com.aricneto.twistytimer.ui.screens.StatsGraphScreen
import com.aricneto.twistytimer.ui.screens.ThemeSelectScreen
import com.aricneto.twistytimer.ui.screens.TimerActions
import com.aricneto.twistytimer.ui.screens.TimerScreen
import com.aricneto.twistytimer.ui.screens.TimerUiState
import com.aricneto.twistytimer.ui.screens.TrainerSubsetScreen
import com.aricneto.twistytimer.utils.AlgUtils
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.PuzzleUtils
import com.aricneto.twistytimer.utils.TTEventBus
import com.aricneto.twistytimer.utils.TTIntent
import com.aricneto.twistytimer.viewmodel.AlgViewModel
import com.aricneto.twistytimer.viewmodel.TimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwistyApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentSubset = navBackStackEntry?.arguments?.getString("subset")
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val timerViewModel: TimerViewModel = viewModel()
    val algViewModel: AlgViewModel = viewModel()

    // App Settings
    val scrambleTextSize by remember { mutableStateOf(Prefs.getInt(R.string.pk_scramble_text_size, 100)) }.let { state ->
        var current by state
        LaunchedEffect(Unit) {
            TTEventBus.events.filter { it.action == TTIntent.ACTION_CHANGED_THEME }.collect {
                current = Prefs.getInt(R.string.pk_scramble_text_size, 100)
            }
        }
        state
    }
    val timerTextSize by remember { mutableStateOf(Prefs.getInt(R.string.pk_timer_text_size, 100)) }.let { state ->
        var current by state
        LaunchedEffect(Unit) {
            TTEventBus.events.filter { it.action == TTIntent.ACTION_CHANGED_THEME }.collect {
                current = Prefs.getInt(R.string.pk_timer_text_size, 100)
            }
        }
        state
    }

    // Global Dialog States
    var solveToShowDetails by remember { mutableStateOf<Solve?>(null) }
    var algorithmToShowDetails by remember { mutableStateOf<Algorithm?>(null) }
    var showCategorySelect by remember { mutableStateOf(false) }
    var showAddTimeManually by remember { mutableStateOf(false) }
    var showHintDialog by remember { mutableStateOf(false) }
    var currentHintText by remember { mutableStateOf("") }
    var isHintLoading by remember { mutableStateOf(false) }
    var showSortOptions by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showClearSessionConfirmation by remember { mutableStateOf(false) }
    var showEditScramble by remember { mutableStateOf(false) }

    val params by timerViewModel.params.collectAsState()
    val scramble by timerViewModel.scramble.collectAsState()
    var editedScramble by remember(scramble) { mutableStateOf(scramble) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.verticalScroll(rememberScrollState()).systemBarsPadding()
            ) {
                Image(
                    painterResource(R.drawable.menu_header),
                    contentDescription = null
                )

                Spacer(Modifier.height(12.dp))

                NavigationDrawerItem(
                    icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_timer_24px), contentDescription = null) },
                    label = { Text("Timer") },
                    selected = currentRoute == "main",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("main")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DrawerSectionHeader(
                    label = "Trainer",
                    icon = R.drawable.ic_outline_track_changes_18px
                )

                TrainerItem("OLL", icon = { Icon(painterResource(R.drawable.ic_oll_black_24dp), null) }, selected = currentRoute == "trainer/{subset}" && currentSubset == "OLL") {
                    scope.launch { drawerState.close() }
                    navController.navigate("trainer/OLL")
                }
                TrainerItem("PLL", icon = { Icon(painterResource(R.drawable.ic_pll_black_24dp), null) }, selected = currentRoute == "trainer/{subset}" && currentSubset == "PLL") {
                    scope.launch { drawerState.close() }
                    navController.navigate("trainer/PLL")
                }
                TrainerItem("CMLL", icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_casino_24px), null) }, selected = currentRoute == "trainer/{subset}" && currentSubset == "CMLL") {
                    scope.launch { drawerState.close() }
                    navController.navigate("trainer/CMLL")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DrawerSectionHeader(
                    label = "Algorithms",
                    icon = R.drawable.ic_outline_library_books_24px
                )

                TrainerItem("OLL Algs", icon = { Icon(painterResource(R.drawable.ic_oll_black_24dp), null) }, selected = currentRoute == "algs/{subset}" && currentSubset == "OLL") {
                    scope.launch { drawerState.close() }
                    navController.navigate("algs/OLL")
                }
                TrainerItem("PLL Algs", icon = { Icon(painterResource(R.drawable.ic_pll_black_24dp), null) }, selected = currentRoute == "algs/{subset}" && currentSubset == "PLL") {
                    scope.launch { drawerState.close() }
                    navController.navigate("algs/PLL")
                }
                TrainerItem("CMLL Algs", icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_casino_24px), null) }, selected = currentRoute == "algs/{subset}" && currentSubset == "CMLL") {
                    scope.launch { drawerState.close() }
                    navController.navigate("algs/CMLL")
                }

                Spacer(Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_settings_24px), null) },
                    label = { Text("Settings") },
                    selected = currentRoute == "settings",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_outline_help_outline_24px), null) },
                    label = { Text("About") },
                    selected = currentRoute == "about",
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("about")
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "main") {
            composable("main") {
                val solves by timerViewModel.solves.collectAsState()
                val isScrambleLoading by timerViewModel.isScrambleLoading.collectAsState()
                val currentTimeMillis by timerViewModel.currentTimeMillis.collectAsState()
                val timerState by timerViewModel.timerState.collectAsState()
                val showQAButtons by timerViewModel.showQAButtons.collectAsState()
                val stats by timerViewModel.timerStats.collectAsState()
                val scrambleDrawable by timerViewModel.scrambleDrawable.collectAsState()
                val selectedSolveIds by timerViewModel.selectedSolveIds.collectAsState()
                val showScrambleHint by timerViewModel.showScrambleHint.collectAsState()
                val manualEntryEnabled by timerViewModel.manualEntryEnabled.collectAsState()

                LaunchedEffect(Unit) {
                    timerViewModel.updateParams(
                        type = params.type ?: PuzzleUtils.TYPE_333,
                        subtype = params.subtype ?: "Normal",
                        mode = 0,
                        history = false
                    )
                    if (scramble == "Generating...") {
                        timerViewModel.generateScramble()
                    }
                }

                val isRunning = timerState == TimerViewModel.TimerState.Running
                val isReady = timerState == TimerViewModel.TimerState.Ready

                MainPagerScreen(
                    currentPuzzle = stringResource(PuzzleUtils.getPuzzleName(params.type).let { if (it == 0) R.string.cube_333_informal else it }),
                    currentCategory = params.subtype ?: "Normal",
                    isRunning = isRunning,
                    swipingEnabled = Prefs.getBoolean(R.string.pk_tab_swiping_enabled, true),
                    selectedSolveCount = selectedSolveIds.size,
                    onSettingsClick = { scope.launch { drawerState.open() } },
                    onCategoryClick = { showCategorySelect = true },
                    onTitleClick = { navController.navigate("puzzle_select") },
                    onClearSelection = { timerViewModel.clearSelection() },
                    onDeleteSelected = { showDeleteConfirmation = true },
                    statsPage = {
                        StatsGraphScreen(
                            currentPuzzle = params.type ?: PuzzleUtils.TYPE_333,
                            currentSubtype = params.subtype ?: "Normal",
                            mode = params.mode,
                            isForCurrentSessionOnly = true
                        )
                    },
                    timerPage = {
                        TimerScreen(
                            uiState = TimerUiState(
                                scramble = scramble,
                                currentTimeMillis = currentTimeMillis,
                                isRunning = isRunning,
                                isReady = isReady,
                                isScrambleLoading = isScrambleLoading,
                                showScrambleHint = showScrambleHint,
                                showRouxHint = showScrambleHint,
                                showRouxSecondBlockHint = showScrambleHint,
                                scrambleDrawable = scrambleDrawable,
                                stats = stats,
                                showQAButtons = showQAButtons,
                                manualEntryEnabled = manualEntryEnabled,
                                scrambleTextSize = scrambleTextSize,
                                timerTextSize = timerTextSize,
                                hideTimeWhileRunning = Prefs.getBoolean(R.string.pk_hide_time_while_running, false),
                                showMillis = Prefs.getBoolean(R.string.pk_show_hi_res_timer, true)
                            ),
                            actions = TimerActions(
                                onTimerDown = { timerViewModel.onTimerDown() },
                                onTimerUp = { timerViewModel.onTimerUp() },
                                onScrambleReset = { timerViewModel.generateScramble() },
                                onScrambleEdit = { showEditScramble = true },
                                onScrambleHintClick = {
                                    scope.launch {
                                        isHintLoading = true
                                        try {
                                            val hintText = withContext(Dispatchers.Default) {
                                                val showXCross = Prefs.getBoolean(R.string.pk_show_scramble_x_cross_hints, false)
                                                val cross = RubiksCubeOptimalCross(TwistyTimer.getAppContext().getString(R.string.optimal_cross))
                                                val crossTip = cross.getTip(scramble)
                                                if (showXCross) {
                                                    val xcross = RubiksCubeOptimalXCross(TwistyTimer.getAppContext().getString(R.string.optimal_x_cross))
                                                    crossTip + "\n\n" + xcross.getTip(scramble)
                                                } else {
                                                    crossTip
                                                }
                                            }
                                            Log.d("TwistyTimer", "=== CROSS HINT ===")
                                            Log.d("TwistyTimer", "Scramble: $scramble")
                                            Log.d("TwistyTimer", "Hint:\n$hintText")
                                            currentHintText = hintText
                                            showHintDialog = true
                                        } catch (_: Exception) {
                                            currentHintText = "Could not generate hint for this scramble."
                                            showHintDialog = true
                                        } finally {
                                            isHintLoading = false
                                        }
                                    }
                                },
                                onRouxHintClick = {
                                    scope.launch {
                                        isHintLoading = true
                                        try {
                                            val hintText = withContext(Dispatchers.Default) {
                                                val fbSolver = RubiksCubeOptimalFirstBlock("Optimal Roux First Block (1x2x3):")
                                                fbSolver.getTip(scramble)
                                            }
                                            Log.d("TwistyTimer", "=== ROUX FIRST BLOCK HINT ===")
                                            Log.d("TwistyTimer", "Scramble: $scramble")
                                            Log.d("TwistyTimer", "Hint:\n$hintText")
                                            currentHintText = hintText
                                            showHintDialog = true
                                        } catch (_: Exception) {
                                            currentHintText = "Could not generate First Block hint for this scramble."
                                            showHintDialog = true
                                        } finally {
                                            isHintLoading = false
                                        }
                                    }
                                },
                                onRouxSecondBlockHintClick = {
                                    scope.launch {
                                        isHintLoading = true
                                        try {
                                            val hintText = withContext(Dispatchers.Default) {
                                                val sbSolver = RubiksCubeOptimalSecondBlock("Optimal Roux Second Block (1x2x3):")
                                                sbSolver.getTip(scramble)
                                            }
                                            Log.d("TwistyTimer", "=== ROUX SECOND BLOCK HINT ===")
                                            Log.d("TwistyTimer", "Scramble: $scramble")
                                            Log.d("TwistyTimer", "Hint:\n$hintText")
                                            currentHintText = hintText
                                            showHintDialog = true
                                        } catch (_: Exception) {
                                            currentHintText = "Could not generate Second Block hint for this scramble."
                                            showHintDialog = true
                                        } finally {
                                            isHintLoading = false
                                        }
                                    }
                                },
                                onRemoveClick = { timerViewModel.removeLastSolve() },
                                onDnfClick = { timerViewModel.applyDnf() },
                                onPlusTwoClick = { timerViewModel.applyPlusTwo() },
                                onCommentClick = { /* Handled via solve detail */ },
                                onManualEntryClick = { showAddTimeManually = true }
                            )
                        )
                    },
                    solvesListPage = {
                        SolvesListScreen(
                            uiState = SolvesListUiState(
                                solves = solves,
                                selectedIds = selectedSolveIds,
                                showClearButton = Prefs.getBoolean(R.string.pk_show_clear_button, true),
                                isHistory = params.history
                            ),
                            actions = SolvesListActions(
                                onSolveClick = { solve -> if (selectedSolveIds.isNotEmpty()) timerViewModel.toggleSelection(solve.id) else solveToShowDetails = solve },
                                onSolveLongClick = { solve -> timerViewModel.toggleSelection(solve.id) },
                                onSearchChange = { query -> timerViewModel.updateParams(params.type, params.subtype, params.mode, params.history, params.subset, query) },
                                onHistoryToggle = { showHistory -> timerViewModel.updateParams(params.type, params.subtype, params.mode, showHistory, params.subset, params.search) },
                                onSortClick = { showSortOptions = true },
                                onClearClick = { showClearSessionConfirmation = true }
                            )
                        )
                    }
                )
            }
            composable(
                "trainer/{subset}",
                arguments = listOf(navArgument("subset") { type = NavType.StringType })
            ) { backStackEntry ->
                val subset = backStackEntry.arguments?.getString("subset") ?: ""
                val solves by timerViewModel.solves.collectAsState()
                val isScrambleLoading by timerViewModel.isScrambleLoading.collectAsState()
                val currentTimeMillis by timerViewModel.currentTimeMillis.collectAsState()
                val timerState by timerViewModel.timerState.collectAsState()
                val showQAButtons by timerViewModel.showQAButtons.collectAsState()
                val stats by timerViewModel.timerStats.collectAsState()
                val scrambleDrawable by timerViewModel.scrambleDrawable.collectAsState()
                val selectedSolveIds by timerViewModel.selectedSolveIds.collectAsState()
                val showScrambleHint by timerViewModel.showScrambleHint.collectAsState()
                val manualEntryEnabled by timerViewModel.manualEntryEnabled.collectAsState()
                val currentCaseId by timerViewModel.currentCaseId.collectAsState()

                LaunchedEffect(subset) {
                    timerViewModel.updateParams(type = subset, subtype = "Normal", mode = 1, history = false, subset = subset)
                    timerViewModel.generateScramble()
                }

                val isRunningTrainer = timerState == TimerViewModel.TimerState.Running
                val isReadyTrainer = timerState == TimerViewModel.TimerState.Ready

                MainPagerScreen(
                    currentPuzzle = "Training $subset",
                    currentCategory = params.subtype ?: "Normal",
                    isRunning = isRunningTrainer,
                    swipingEnabled = Prefs.getBoolean(R.string.pk_tab_swiping_enabled, true),
                    selectedSolveCount = selectedSolveIds.size,
                    onSettingsClick = { scope.launch { drawerState.open() } },
                    onCategoryClick = { showCategorySelect = true },
                    onTitleClick = { navController.navigate("trainer/$subset/select") },
                    onClearSelection = { timerViewModel.clearSelection() },
                    onDeleteSelected = { showDeleteConfirmation = true },
                    statsPage = {
                        StatsGraphScreen(
                            currentPuzzle = subset,
                            currentSubtype = params.subtype ?: "Normal",
                            mode = params.mode,
                            isForCurrentSessionOnly = true
                        )
                    },
                    timerPage = {
                        TimerScreen(
                            uiState = TimerUiState(
                                scramble = scramble,
                                currentTimeMillis = currentTimeMillis,
                                isRunning = isRunningTrainer,
                                isReady = isReadyTrainer,
                                isScrambleLoading = isScrambleLoading,
                                showScrambleHint = showScrambleHint,
                                showRouxHint = showScrambleHint,
                                scrambleDrawable = scrambleDrawable,
                                stats = stats,
                                showQAButtons = showQAButtons,
                                manualEntryEnabled = manualEntryEnabled,
                                scrambleTextSize = scrambleTextSize,
                                timerTextSize = timerTextSize,
                                hideTimeWhileRunning = Prefs.getBoolean(R.string.pk_hide_time_while_running, false),
                                showMillis = Prefs.getBoolean(R.string.pk_show_hi_res_timer, true)
                            ),
                            actions = TimerActions(
                                onTimerDown = { timerViewModel.onTimerDown() },
                                onTimerUp = { timerViewModel.onTimerUp() },
                                onScrambleReset = { timerViewModel.generateScramble() },
                                onScrambleEdit = { /* Not needed in trainer */ },
                                onScrambleHintClick = {
                                    if (currentCaseId != null) {
                                        scope.launch {
                                            val algs = withContext(Dispatchers.IO) { TwistyTimer.getAlgRepository().getAllAlgorithms() }
                                            algorithmToShowDetails = algs.find { it.id == currentCaseId }
                                        }
                                    } else {
                                        navController.navigate("trainer/$subset/select")
                                    }
                                },
                                onRouxHintClick = {
                                    scope.launch {
                                        isHintLoading = true
                                        try {
                                            val hintText = withContext(Dispatchers.Default) {
                                                val fbSolver = RubiksCubeOptimalFirstBlock("Optimal Roux First Block (1x2x3):")
                                                fbSolver.getTip(scramble)
                                            }
                                            currentHintText = hintText
                                            showHintDialog = true
                                        } catch (_: Exception) {
                                            currentHintText = "Could not generate First Block hint for this scramble."
                                            showHintDialog = true
                                        } finally {
                                            isHintLoading = false
                                        }
                                    }
                                },
                                onRemoveClick = { timerViewModel.removeLastSolve() },
                                onDnfClick = { timerViewModel.applyDnf() },
                                onPlusTwoClick = { timerViewModel.applyPlusTwo() },
                                onManualEntryClick = { showAddTimeManually = true }
                            )
                        )
                    },
                    solvesListPage = {
                        SolvesListScreen(
                            uiState = SolvesListUiState(
                                solves = solves,
                                selectedIds = selectedSolveIds,
                                showClearButton = Prefs.getBoolean(R.string.pk_show_clear_button, true),
                                isHistory = params.history
                            ),
                            actions = SolvesListActions(
                                onSolveClick = { solve -> if (selectedSolveIds.isNotEmpty()) timerViewModel.toggleSelection(solve.id) else solveToShowDetails = solve },
                                onSolveLongClick = { solve -> timerViewModel.toggleSelection(solve.id) },
                                onSearchChange = { query -> timerViewModel.updateParams(params.type, params.subtype, params.mode, params.history, params.subset, query) },
                                onHistoryToggle = { showHistory -> timerViewModel.updateParams(params.type, params.subtype, params.mode, showHistory, params.subset, params.search) },
                                onSortClick = { showSortOptions = true },
                                onClearClick = { showClearSessionConfirmation = true }
                            )
                        )
                    }
                )
            }
            composable(
                "trainer/{subset}/select",
                arguments = listOf(navArgument("subset") { type = NavType.StringType })
            ) { backStackEntry ->
                val subset = backStackEntry.arguments?.getString("subset") ?: ""
                val algorithms by algViewModel.algorithms.collectAsState()

                LaunchedEffect(subset) { algViewModel.setSubset(subset) }

                TrainerSubsetScreen(
                    subsetName = subset,
                    categoryName = "Normal",
                    algorithms = algorithms,
                    onBackClick = { navController.popBackStack(); timerViewModel.generateScramble() },
                    onSettingsClick = { scope.launch { drawerState.open() } },
                    onItemClick = { algorithmToShowDetails = it }
                )
            }
            composable(
                "algs/{subset}",
                arguments = listOf(navArgument("subset") { type = NavType.StringType })
            ) { backStackEntry ->
                val subset = backStackEntry.arguments?.getString("subset") ?: ""
                val algorithms by algViewModel.algorithms.collectAsState()

                LaunchedEffect(subset) { algViewModel.setSubset(subset) }

                AlgListScreen(
                    algorithms = algorithms,
                    subsetName = subset,
                    onItemClick = { algorithmToShowDetails = it },
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { scope.launch { drawerState.open() } }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onThemeClick = { navController.navigate("theme_select") },
                    onLanguageClick = { navController.navigate("language_select") },
                    onBackupClick = { navController.navigate("export_import") },
                    onColorSchemeClick = { navController.navigate("color_scheme") }
                )
            }
            composable("about") {
                AboutScreen(onBackClick = { navController.popBackStack() })
            }
            composable("theme_select") {
                ThemeSelectScreen(onBackClick = { navController.popBackStack() })
            }
            composable("language_select") {
                LanguageSelectScreen(onBackClick = { navController.popBackStack() })
            }
            composable("color_scheme") {
                ColorSchemeScreen(onBackClick = { navController.popBackStack() })
            }
            composable("export_import") {
                ExportImportScreen(onBackClick = { navController.popBackStack() })
            }
            composable("puzzle_select") {
                val p by timerViewModel.params.collectAsState()
                PuzzleSelectScreen(
                    currentPuzzle = p.type ?: PuzzleUtils.TYPE_333,
                    onPuzzleSelected = { type, category ->
                        timerViewModel.updateParams(type, category, 0, false)
                        timerViewModel.generateScramble()
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        // Global Dialogs
        if (showCategorySelect) {
            CategorySelectComposeDialog(
                puzzleType = params.type ?: PuzzleUtils.TYPE_333,
                currentCategory = params.subtype ?: "Normal",
                mode = params.mode,
                onDismiss = { showCategorySelect = false },
                onCategorySelected = { category ->
                    timerViewModel.updateParams(params.type, category, params.mode, params.history, params.subset)
                    timerViewModel.generateScramble()
                }
            )
        }

        if (showAddTimeManually) {
            AddTimeComposeDialog(
                onDismiss = { showAddTimeManually = false },
                onSave = { t, p, c -> timerViewModel.addManualSolve(t, p, c); showAddTimeManually = false }
            )
        }

        if (showHintDialog) {
            HintComposeDialog(hint = currentHintText, onDismiss = { showHintDialog = false })
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(stringResource(R.string.delete_dialog_confirmation_title)) },
                confirmButton = {
                    TextButton(onClick = {
                        timerViewModel.deleteSolves(timerViewModel.selectedSolveIds.value.toList())
                        timerViewModel.clearSelection()
                        showDeleteConfirmation = false
                    }) { Text(stringResource(R.string.delete_dialog_confirmation_button)) }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.delete_dialog_cancel_button)) } }
            )
        }

        if (showClearSessionConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearSessionConfirmation = false },
                title = { Text(stringResource(R.string.remove_session_title)) },
                text = { Text(stringResource(R.string.remove_session_confirmation_content)) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val currentType = params.type ?: PuzzleUtils.TYPE_333
                            val currentSubtype = params.subtype ?: "Normal"
                            TwistyTimer.getSolveRepository().deleteAllFromSession(currentType, currentSubtype, params.mode)
                            TTIntent.broadcast(TTIntent.CATEGORY_TIME_DATA_CHANGES, TTIntent.ACTION_TIMES_MODIFIED)
                            showClearSessionConfirmation = false
                        }
                    }) { Text(stringResource(R.string.action_remove)) }
                },
                dismissButton = { TextButton(onClick = { showClearSessionConfirmation = false }) { Text(stringResource(R.string.action_cancel)) } }
            )
        }

        if (showEditScramble) {
            AlertDialog(
                onDismissRequest = { showEditScramble = false },
                title = { Text(stringResource(R.string.edit_scramble)) },
                text = { OutlinedTextField(value = editedScramble, onValueChange = { editedScramble = it }, modifier = Modifier.fillMaxWidth()) },
                confirmButton = { TextButton(onClick = { timerViewModel.setScramble(editedScramble); showEditScramble = false }) { Text(stringResource(R.string.action_done)) } },
                dismissButton = { TextButton(onClick = { showEditScramble = false }) { Text(stringResource(R.string.action_cancel)) } }
            )
        }

        if (showSortOptions) {
            SortOptionsDialog(
                currentKey = params.orderByKey,
                currentDir = params.orderByDir,
                onSortSelected = { key, dir ->
                    timerViewModel.updateParams(params.type, params.subtype, params.mode, params.history, params.subset, params.search, key, dir)
                    showSortOptions = false
                },
                onDismiss = { showSortOptions = false }
            )
        }

        if (solveToShowDetails != null) {
            SolveDetailDialog(
                solve = solveToShowDetails!!,
                onDismiss = { solveToShowDetails = null },
                onDelete = { timerViewModel.deleteSolves(listOf(solveToShowDetails!!.id)); solveToShowDetails = null },
                onPenaltyChange = { p -> val u = solveToShowDetails!!.copy(penalty = p); timerViewModel.updateSolve(u); solveToShowDetails = u },
                onCommentChange = { c -> val u = solveToShowDetails!!.copy(comment = c); timerViewModel.updateSolve(u); solveToShowDetails = u },
                onToggleHistory = { h -> val u = solveToShowDetails!!.copy(history = h); timerViewModel.updateSolve(u); solveToShowDetails = u }
            )
        }

        if (algorithmToShowDetails != null) {
            AlgorithmDetailDialog(
                algorithm = algorithmToShowDetails!!,
                onDismiss = { algorithmToShowDetails = null },
                onEditAlg = { n -> val u = algorithmToShowDetails!!.copy(algs = n); algViewModel.updateAlgorithm(u); algorithmToShowDetails = u },
                onProgressChange = { p -> val u = algorithmToShowDetails!!.copy(progress = p); algViewModel.updateAlgorithm(u); algorithmToShowDetails = u },
                onResetAlg = { val d = AlgUtils.getDefaultAlgs(algorithmToShowDetails!!.subset, algorithmToShowDetails!!.name); val u = algorithmToShowDetails!!.copy(algs = d); algViewModel.updateAlgorithm(u); algorithmToShowDetails = u }
            )
        }

        if (isHintLoading) {
            AlertDialog(
                onDismissRequest = { },
                confirmButton = { },
                title = { Text("Calculating Hints...") },
                text = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            )
        }
    }
}

@Composable
private fun DrawerSectionHeader(label: String, icon: Int) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = ImageVector.vectorResource(icon), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TrainerItem(
    label: String,
    icon: @Composable () -> Unit = {},
    selected: Boolean = false,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = icon,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
private fun SortOptionsDialog(
    currentKey: String,
    currentDir: String,
    onSortSelected: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Solves") },
        text = {
            Column {
                SortOptionItem("Newest first", currentKey == SolveRepository.KEY_DATE && currentDir == SolveRepository.DIR_DESC) { onSortSelected(SolveRepository.KEY_DATE, SolveRepository.DIR_DESC) }
                SortOptionItem("Oldest first", currentKey == SolveRepository.KEY_DATE && currentDir == SolveRepository.DIR_ASC) { onSortSelected(SolveRepository.KEY_DATE, SolveRepository.DIR_ASC) }
                SortOptionItem("Best time first", currentKey == SolveRepository.KEY_TIME && currentDir == SolveRepository.DIR_ASC) { onSortSelected(SolveRepository.KEY_TIME, SolveRepository.DIR_ASC) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun SortOptionItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = Modifier.clickable { onClick() },
        trailingContent = { if (isSelected) RadioButton(selected = true, onClick = null) }
    )
}
