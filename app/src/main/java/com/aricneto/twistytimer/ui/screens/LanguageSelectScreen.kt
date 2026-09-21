package com.aricneto.twistytimer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.aricneto.twistify.R
import com.aricneto.twistytimer.ui.theme.LocalTwistyColors
import com.aricneto.twistytimer.utils.LocaleUtils
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = remember { LocaleUtils.localeHashMap }
    var currentLocale by remember { mutableStateOf(LocaleUtils.locale ?: "en_US") }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text("Select Language") },
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
            items(languages.entries.toList()) { entry ->
                val code = entry.key
                val nameRes = entry.value.first
                val flagRes = entry.value.second
                
                ListItem(
                    headlineContent = { Text(stringResource(nameRes)) },
                    supportingContent = { Text(code) },
                    leadingContent = {
                        Icon(
                            imageVector = ImageVector.vectorResource(flagRes),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.Unspecified
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = code == currentLocale,
                            onClick = null
                        )
                    },
                    modifier = Modifier.clickable {
                        LocaleUtils.locale = code
                        currentLocale = code
                        // App might need recreation here
                    }
                )
            }
        }
    }
}
