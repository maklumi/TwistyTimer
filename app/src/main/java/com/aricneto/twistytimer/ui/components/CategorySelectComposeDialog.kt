package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aricneto.twistify.R
import com.aricneto.twistytimer.TwistyTimer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectComposeDialog(
    puzzleType: String,
    currentCategory: String,
    mode: Int,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val repository = remember { TwistyTimer.getSolveRepository() }
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf(listOf<String>()) }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            val list = repository.getAllSubtypesFromType(puzzleType, mode)
            categories = if (list.isNotEmpty()) list else listOf("Normal")
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Category", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_outline_add_18px), contentDescription = "Add")
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                items(categories) { category ->
                    ListItem(
                        headlineContent = { Text(category) },
                        trailingContent = {
                            if (category == currentCategory) {
                                RadioButton(selected = true, onClick = null)
                            }
                        },
                        modifier = Modifier.clickable {
                            onCategorySelected(category)
                            onDismiss()
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        scope.launch {
                            repository.insertSolve(
                                type = puzzleType,
                                subtype = newCategoryName,
                                time = 1L,
                                date = 0L,
                                scramble = "",
                                penalty = 10, // PENALTY_HIDETIME
                                comment = "",
                                history = true,
                                mode = mode
                            )
                            refresh()
                            showCreateDialog = false
                        }
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
