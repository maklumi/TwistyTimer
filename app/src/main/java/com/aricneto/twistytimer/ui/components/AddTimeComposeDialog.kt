package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aricneto.twistytimer.utils.PuzzleUtils

@Composable
fun AddTimeComposeDialog(
    onDismiss: () -> Unit,
    onSave: (Long, Int, String) -> Unit
) {
    var timeInput by remember { mutableStateOf("") }
    var selectedPenalty by remember { mutableIntStateOf(PuzzleUtils.NO_PENALTY) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Time Manually") },
        text = {
            Column {
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it },
                    label = { Text("Time (e.g. 1:23.45)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text("Penalty", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PenaltyChip("None", selectedPenalty == PuzzleUtils.NO_PENALTY) { selectedPenalty = PuzzleUtils.NO_PENALTY }
                    PenaltyChip("+2", selectedPenalty == PuzzleUtils.PENALTY_PLUSTWO) { selectedPenalty = PuzzleUtils.PENALTY_PLUSTWO }
                    PenaltyChip("DNF", selectedPenalty == PuzzleUtils.PENALTY_DNF) { selectedPenalty = PuzzleUtils.PENALTY_DNF }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val timeMillis = PuzzleUtils.parseTime(timeInput).toLong()
                if (timeMillis > 0 || selectedPenalty == PuzzleUtils.PENALTY_DNF) {
                    onSave(timeMillis, selectedPenalty, comment)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PenaltyChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) }
    )
}
