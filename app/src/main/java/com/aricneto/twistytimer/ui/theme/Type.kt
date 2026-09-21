package com.aricneto.twistytimer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aricneto.twistify.R

val Lato = FontFamily(
    Font(R.font.lato, FontWeight.Normal),
    Font(R.font.lato_bold, FontWeight.Bold)
)

val Quicksand = FontFamily(
    Font(R.font.quicksand, FontWeight.Normal),
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_bold, FontWeight.Bold)
)

val OverpassMono = FontFamily(
    Font(R.font.overpass_mono_semibold, FontWeight.SemiBold)
)

fun getTypographyForStyle(style: String): Typography {
    val baseFont = when (style) {
        "pessoa", "burgess", "lou", "bowie" -> Lato
        "brie", "matsson", "isakov", "adams" -> Quicksand
        "irwin", "tarkovsky", "ebert" -> OverpassMono
        "tolkien", "asimov", "kubrick" -> FontFamily.Serif
        else -> Quicksand
    }
    
    return Typography(
        bodyLarge = TextStyle(
            fontFamily = baseFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        titleLarge = TextStyle(
            fontFamily = baseFont,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = baseFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelLarge = TextStyle(
            fontFamily = baseFont,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        )
    )
}

val Typography = getTypographyForStyle("default")
