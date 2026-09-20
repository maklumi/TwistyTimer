package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aricneto.twistify.R

@Composable
fun QAButtons(
    onRemoveClick: () -> Unit,
    onDnfClick: () -> Unit,
    onPlusTwoClick: () -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onRemoveClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_clear_black_24dp),
                contentDescription = "Remove",
                tint = color
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        IconButton(onClick = onDnfClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_outline_block_24px),
                contentDescription = "DNF",
                tint = color
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        IconButton(onClick = onPlusTwoClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_outline_flag_24px),
                contentDescription = "+2",
                tint = color
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        IconButton(onClick = onCommentClick) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_outline_add_comment_24px),
                contentDescription = "Comment",
                tint = color
            )
        }
    }
}
