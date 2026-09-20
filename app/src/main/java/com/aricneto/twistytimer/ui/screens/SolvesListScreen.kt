package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aricneto.twistify.R
import com.aricneto.twistytimer.items.Solve
import com.aricneto.twistytimer.utils.PuzzleUtils
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun SolvesListScreen(
    solves: List<Solve>,
    selectedIds: Set<Long>,
    onSolveClick: (Solve) -> Unit,
    onSolveLongClick: (Solve) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortClick: () -> Unit,
    showClearButton: Boolean = false,
    onClearClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchChange(it)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search comments...") },
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_outline_insert_comment_24px), // Placeholder for search icon
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            if (showClearButton) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_outline_delete_sweep_24px),
                        contentDescription = "Clear All"
                    )
                }
            }

            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_outline_list_alt_24px), // Placeholder for sort icon
                    contentDescription = "Sort"
                )
            }
        }
        LazyVerticalGrid(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3)
        ) {
            items(solves) { solve ->
                SolveItem(
                    solve = solve,
                    isSelected = selectedIds.contains(solve.id),
                    onClick = { onSolveClick(solve) },
                    onLongClick = { onSolveLongClick(solve) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SolveItem(
    solve: Solve,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = stringResource(R.string.shortDateFormat)
    val dateStr = SimpleDateFormat(dateFormat).format(Date(solve.date))
    
    val timeStr = if (solve.penalty == PuzzleUtils.PENALTY_DNF) {
        stringResource(R.string.do_not_finished)
    } else {
        PuzzleUtils.convertTimeToString(solve.time.toLong(), PuzzleUtils.FORMAT_NO_MILLI)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left part: Time and Penalty
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeStr,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (solve.penalty == PuzzleUtils.PENALTY_DNF) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                    )
                    
                    if (solve.penalty == PuzzleUtils.PENALTY_PLUSTWO) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "+2",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                /*
                if (solve.scramble.isNotEmpty()) {
                    Text(
                        text = solve.scramble,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } */
            }

            // Right part: Date and Icons
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
                
                if (solve.comment.isNotEmpty()) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_outline_insert_comment_24px),
                        contentDescription = "Comment",
                        modifier = Modifier.padding(top = 8.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
