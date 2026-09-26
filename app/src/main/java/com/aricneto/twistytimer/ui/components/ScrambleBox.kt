package com.aricneto.twistytimer.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aricneto.twistify.R

@Composable
fun ScrambleBox(
    scramble: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    showHint: Boolean = false,
    showRouxHint: Boolean = false,
    showRouxSecondBlockHint: Boolean = false,
    showManualEntry: Boolean = false,
    onResetClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onHintClick: () -> Unit = {},
    onRouxHintClick: () -> Unit = {},
    onRouxSecondBlockHintClick: () -> Unit = {},
    onManualEntryClick: () -> Unit = {},
    fontSize: TextUnit = 18.sp
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = scramble,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    if (showHint) {
                        IconButton(onClick = onHintClick) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_outline_wb_incandescent_24px),
                                contentDescription = "Cross Hint",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                    if (showRouxHint) {
                        IconButton(onClick = onRouxHintClick) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_outline_grid_on_24px),
                                contentDescription = "Roux First Block Hint",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (showRouxSecondBlockHint) {
                        IconButton(onClick = onRouxSecondBlockHintClick) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_outline_grid_on_2_24px),
                                contentDescription = "Roux Second Block Hint",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Scramble", scramble)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Scramble copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_clippy),
                            contentDescription = "Copy Scramble",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }

                    if (showManualEntry) {
                        IconButton(onClick = onManualEntryClick) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_outline_alarm_add_24dp),
                                contentDescription = "Manual Entry",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_outline_edit_24px),
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                    IconButton(onClick = onResetClick) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_outline_cached_24px),
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
