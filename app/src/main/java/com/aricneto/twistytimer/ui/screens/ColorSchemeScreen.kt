package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.aricneto.twistify.R
import com.aricneto.twistytimer.ui.theme.LocalTwistyColors
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.TTEventBus
import com.aricneto.twistytimer.utils.TTIntent
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSchemeScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUpdateVersion = remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        TTEventBus.events
            .filter { it.action == TTIntent.ACTION_CHANGED_THEME }
            .collect { currentUpdateVersion.intValue++ }
    }

    val faces = listOf(
        "Top (U)" to R.string.pk_cube_top_color,
        "Bottom (D)" to R.string.pk_cube_down_color,
        "Left (L)" to R.string.pk_cube_left_color,
        "Right (R)" to R.string.pk_cube_right_color,
        "Front (F)" to R.string.pk_cube_front_color,
        "Back (B)" to R.string.pk_cube_back_color
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text("Color Scheme") },
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
        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            items(faces.size) { index ->
                val (label, resId) = faces[index]
                key(currentUpdateVersion.intValue) {
                    val colorHex = Prefs.getString(resId, "FFFFFF") ?: "FFFFFF"
                    val color = try { Color("#$colorHex".toColorInt()) } catch (_: Exception) { Color.White }
                    
                    ListItem(
                        headlineContent = { Text(label) },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(color, CircleShape)
                            )
                        },
                        modifier = Modifier.clickable {
                            // For a real app, I'd implement a Color Picker Dialog
                            // For now, let's just rotate through some standard cube colors
                            val current = Prefs.getString(resId, "FFFFFF") ?: "FFFFFF"
                            val next = when(current) {
                                "FFFFFF" -> "FDD835" // Yellow
                                "FDD835" -> "02D040" // Green
                                "02D040" -> "304FFE" // Blue
                                "304FFE" -> "EC0000" // Red
                                "EC0000" -> "FF8B24" // Orange
                                else -> "FFFFFF"
                            }
                            Prefs.edit().putString(resId, next).apply()
                            TTIntent.broadcast(TTIntent.CATEGORY_UI_INTERACTIONS, TTIntent.ACTION_CHANGED_THEME)
                        }
                    )
                }
            }
            
            item {
                key(currentUpdateVersion.intValue) {
                    TextButton(
                        onClick = {
                            Prefs.edit()
                                .putString(R.string.pk_cube_top_color, "FFFFFF")
                                .putString(R.string.pk_cube_down_color, "FDD835")
                                .putString(R.string.pk_cube_left_color, "FF8B24")
                                .putString(R.string.pk_cube_right_color, "EC0000")
                                .putString(R.string.pk_cube_front_color, "02D040")
                                .putString(R.string.pk_cube_back_color, "304FFE")
                                .apply()
                            TTIntent.broadcast(TTIntent.CATEGORY_UI_INTERACTIONS, TTIntent.ACTION_CHANGED_THEME)
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Reset to defaults")
                    }
                }
            }
        }
    }
}
