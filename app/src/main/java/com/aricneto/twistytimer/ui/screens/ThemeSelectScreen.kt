package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.aricneto.twistytimer.ui.theme.LocalTwistyColors

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
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars,
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
        },
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    LocalTwistyColors.current.backgroundGradientStart,
                    LocalTwistyColors.current.backgroundGradientEnd
                )
            )
        )
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

            item(span = { GridItemSpan(maxLineSpan) }) {
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
    
    val gradientBrush = remember(theme.prefName) {
        val styleRes = ThemeUtils.getThemeStyleRes(theme.prefName)
        val gradient = ThemeUtils.fetchBackgroundGradient(context, styleRes)
        val colors = gradient.colors ?: intArrayOf(0xFF3F51B5.toInt(), 0xFF3F51B5.toInt())
        
        Brush.verticalGradient(
            colors = colors.map { Color(it) }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(gradientBrush, RoundedCornerShape(12.dp))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
        )
        Text(
            text = theme.name,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
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
