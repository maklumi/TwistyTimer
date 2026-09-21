package com.aricneto.twistytimer.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.aricneto.twistify.R
import com.aricneto.twistytimer.ui.TwistyApp
import com.aricneto.twistytimer.ui.theme.TwistyTheme
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.TTEventBus
import com.aricneto.twistytimer.utils.TTIntent
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTextStyle
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTheme
import kotlinx.coroutines.flow.filter

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(preferredTheme)

        // Set text styling
        if (Prefs.getString(R.string.pk_text_style, "default") != "default") {
            theme.applyStyle(preferredTextStyle, true)
        }

        enableEdgeToEdge()

        setContent {
            val currentThemeName = remember { mutableStateOf(Prefs.getString(R.string.pk_theme, "indigo") ?: "indigo") }
            val currentTextStyle = remember { mutableStateOf(Prefs.getString(R.string.pk_text_style, "default") ?: "default") }
            
            LaunchedEffect(Unit) {
                TTEventBus.events
                    .filter { it.action == TTIntent.ACTION_CHANGED_THEME }
                    .collect {
                        currentThemeName.value = Prefs.getString(R.string.pk_theme, "indigo") ?: "indigo"
                        currentTextStyle.value = Prefs.getString(R.string.pk_text_style, "default") ?: "default"
                    }
            }

            TwistyTheme(
                themeName = currentThemeName.value,
                textStyle = currentTextStyle.value
            ) {
                TwistyApp()
            }
        }
    }
}
