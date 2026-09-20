package com.aricneto.twistytimer.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aricneto.twistify.R
import com.aricneto.twistytimer.ui.TwistyApp
import com.aricneto.twistytimer.ui.theme.TwistyTheme
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTextStyle
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTheme

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
            TwistyTheme {
                TwistyApp()
            }
        }
    }
}
