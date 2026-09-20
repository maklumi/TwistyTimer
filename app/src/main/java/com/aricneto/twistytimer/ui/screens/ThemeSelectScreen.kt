package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aricneto.twistify.R
import com.aricneto.twistytimer.items.Theme
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.TTIntent
import com.aricneto.twistytimer.utils.ThemeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTheme by remember { mutableStateOf(Prefs.getString(R.string.pk_theme, "indigo") ?: "indigo") }
    var currentTextStyle by remember { mutableStateOf(Prefs.getString(R.string.pk_text_style, "default") ?: "default") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Theme") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back_black_24dp),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = modifier.padding(paddingValues).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Colors",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            items(ThemeUtils.allThemes) { theme ->
                theme?.let {
                    ThemeItem(
                        theme = it,
                        isSelected = it.prefName == currentTheme,
                        onClick = {
                            if (it.prefName != currentTheme) {
                                Prefs.edit().putString(R.string.pk_theme, it.prefName).apply()
                                currentTheme = it.prefName
                                TTIntent.broadcast(TTIntent.CATEGORY_UI_INTERACTIONS, TTIntent.ACTION_CHANGED_THEME)
                            }
                        }
                    )
                }
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    "Text Styles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(ThemeUtils.getAllTextStyles(context)) { style ->
                style?.let {
                    TextStyleItem(
                        style = it,
                        isSelected = it.prefName == currentTextStyle,
                        onClick = {
                            if (it.prefName != currentTextStyle) {
                                Prefs.edit().putString(R.string.pk_text_style, it.prefName).apply()
                                currentTextStyle = it.prefName
                                TTIntent.broadcast(TTIntent.CATEGORY_UI_INTERACTIONS, TTIntent.ACTION_CHANGED_THEME)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeItem(
    theme: Theme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    // In a full implementation, we'd extract actual colors from the theme resource
    // For now, we'll use a placeholder color based on prefName
    val color = remember(theme.prefName) {
        when(theme.prefName) {
            "indigo" -> Color(0xFF3F51B5)
            "purple" -> Color(0xFF9C27B0)
            "teal" -> Color(0xFF009688)
            "pink" -> Color(0xFFE91E63)
            "red" -> Color(0xFFF44336)
            "brown" -> Color(0xFF795548)
            "blue" -> Color(0xFF2196F3)
            "cyan" -> Color(0xFF00BCD4)
            "black" -> Color.Black
            "orange" -> Color(0xFFFF9800)
            "green" -> Color(0xFF4CAF50)
            "white" -> Color.White
            else -> Color.Gray
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color, RoundedCornerShape(12.dp))
                .let { 
                    if (isSelected) it.background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    else it
                }
                .let {
                    if (isSelected) it.padding(4.dp).background(color, RoundedCornerShape(8.dp))
                    else it
                }
        )
        Text(
            text = theme.name,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun TextStyleItem(
    style: Theme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = style.name,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
