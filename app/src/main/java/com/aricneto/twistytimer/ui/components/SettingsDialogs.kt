package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NumberPickerDialog(
    title: String,
    initialValue: Int,
    minValue: Int,
    maxValue: Int,
    onValueSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentValue by remember { mutableIntStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Value: $currentValue", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = currentValue.toFloat(),
                    onValueChange = { currentValue = it.toInt() },
                    valueRange = minValue.toFloat()..maxValue.toFloat(),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onValueSelected(currentValue) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
