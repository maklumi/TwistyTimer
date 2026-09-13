package com.aricneto.twistytimer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aricneto.twistify.R

@Composable
fun TwistyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSettingsClick: () -> Unit = {},
    onCategoryClick: (() -> Unit)? = null,
    showSpinnerIcon: Boolean = false
) {
    ElevatedCard(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .height(56.dp) // Legay actionBarSize
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Settings button (Left)
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_outline_settings_24px),
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Title and Subtitle (Center)
            Column(
                modifier = Modifier
                    .clickable { /* Handle spinner click if needed */ }
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (showSpinnerIcon) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_outline_arrow_drop_down_24px),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).padding(start = 4.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
            }

            // Category button (Right)
            if (onCategoryClick != null) {
                IconButton(
                    onClick = onCategoryClick,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_outline_category_24),
                        contentDescription = "Category",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
