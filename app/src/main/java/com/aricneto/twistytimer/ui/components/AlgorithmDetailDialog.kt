package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aricneto.twistytimer.items.Algorithm
import com.aricneto.twistytimer.utils.AlgUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlgorithmDetailDialog(
    algorithm: Algorithm,
    onDismiss: () -> Unit,
    onEditAlg: (String) -> Unit,
    onProgressChange: (Int) -> Unit,
    onResetAlg: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val pState = remember(algorithm) { AlgUtils.getCaseState(context, algorithm.subset, algorithm.name) }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editedAlgs by remember { mutableStateOf(algorithm.algs) }
    var currentProgress by remember { mutableFloatStateOf(algorithm.progress.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = algorithm.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = algorithm.subset,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CubeComponent(
                    state = pState,
                    modifier = Modifier.size(140.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = algorithm.algs,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Learning Progress: ${currentProgress.toInt()}%",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = currentProgress,
                onValueChange = { currentProgress = it },
                onValueChangeFinished = { onProgressChange(currentProgress.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { showEditDialog = true }) {
                    Text("Edit Algorithm")
                }
                OutlinedButton(onClick = onResetAlg) {
                    Text("Reset to Default")
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Algorithm") },
            text = {
                OutlinedTextField(
                    value = editedAlgs,
                    onValueChange = { editedAlgs = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter algorithm(s)...") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditAlg(editedAlgs)
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
