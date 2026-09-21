package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aricneto.twistify.R
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.ui.theme.LocalTwistyColors
import com.aricneto.twistytimer.utils.PuzzleUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PuzzleSelectScreen(
    currentPuzzle: String,
    onPuzzleSelected: (String, String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val puzzles = stringArrayResource(R.array.puzzles)
    val puzzleTypes = remember {
        List(puzzles.size) { i -> PuzzleUtils.getPuzzleInPosition(i) }
    }
    
    var selectedPuzzleType by remember { mutableStateOf(currentPuzzle) }
    var categories by remember { mutableStateOf(listOf("Normal")) }
    
    val repository = remember { TwistyTimer.getSolveRepository() }
    val scope = rememberCoroutineScope()

    var showCreateCategory by remember { mutableStateOf(false) }
    var showRenameCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var renameCategoryName by remember { mutableStateOf("") }
    
    var categoryToEdit by remember { mutableStateOf<String?>(null) }
    var showCategoryOptions by remember { mutableStateOf(false) }

    fun refreshCategories() {
        scope.launch {
            val list = repository.getAllSubtypesFromType(selectedPuzzleType, 0)
            categories = if (list.isNotEmpty()) list else listOf("Normal")
        }
    }

    LaunchedEffect(selectedPuzzleType) {
        refreshCategories()
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text("Select Puzzle") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back_black_24dp),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateCategory = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_outline_add_18px),
                            contentDescription = "Add Category"
                        )
                    }
                }
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
        Row(modifier = modifier.padding(paddingValues).fillMaxSize()) {
            // Left: Puzzle List
            LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight()) {
                items(puzzleTypes.size) { index ->
                    val type = puzzleTypes[index]
                    val label = puzzles[index]
                    NavigationDrawerItem(
                        label = { Text(label) },
                        selected = selectedPuzzleType == type,
                        onClick = { selectedPuzzleType = type },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            
            VerticalDivider()

            // Right: Category List
            LazyColumn(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                item {
                    Text(
                        "Categories",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(categories) { category ->
                    Box {
                        ListItem(
                            headlineContent = { Text(category) },
                            modifier = Modifier.combinedClickable(
                                onClick = { onPuzzleSelected(selectedPuzzleType, category) },
                                onLongClick = {
                                    categoryToEdit = category
                                    showCategoryOptions = true
                                }
                            )
                        )
                        
                        if (showCategoryOptions && categoryToEdit == category) {
                            DropdownMenu(
                                expanded = showCategoryOptions,
                                onDismissRequest = { showCategoryOptions = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        renameCategoryName = category
                                        showRenameCategory = true
                                        showCategoryOptions = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        scope.launch {
                                            repository.deleteSubtype(selectedPuzzleType, category, 0)
                                            refreshCategories()
                                        }
                                        showCategoryOptions = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateCategory) {
        AlertDialog(
            onDismissRequest = { showCreateCategory = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.length in 2..32) {
                        scope.launch {
                            repository.insertSolve(
                                type = selectedPuzzleType,
                                subtype = newCategoryName,
                                time = 1L,
                                date = 0L,
                                scramble = "",
                                penalty = PuzzleUtils.PENALTY_HIDETIME.toLong(),
                                comment = "",
                                history = true,
                                mode = 0
                            )
                            refreshCategories()
                            showCreateCategory = false
                            newCategoryName = ""
                        }
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCategory = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRenameCategory) {
        AlertDialog(
            onDismissRequest = { showRenameCategory = false },
            title = { Text("Rename Category") },
            text = {
                OutlinedTextField(
                    value = renameCategoryName,
                    onValueChange = { renameCategoryName = it },
                    label = { Text("New Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameCategoryName.length in 2..32 && categoryToEdit != null) {
                        scope.launch {
                            repository.renameSubtype(
                                renameCategoryName,
                                selectedPuzzleType,
                                categoryToEdit!!,
                                0
                            )
                            refreshCategories()
                            showRenameCategory = false
                            categoryToEdit = null
                        }
                    }
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameCategory = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
