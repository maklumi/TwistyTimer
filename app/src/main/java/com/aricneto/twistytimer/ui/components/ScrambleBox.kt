package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    showManualEntry: Boolean = false,
    onResetClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onHintClick: () -> Unit = {},
    onManualEntryClick: () -> Unit = {},
    fontSize: TextUnit = 18.sp
) {
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
                if (showHint) {
                    IconButton(onClick = onHintClick) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_outline_wb_incandescent_24px),
                            contentDescription = "Hint",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Row {
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
