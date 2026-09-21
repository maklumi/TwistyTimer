package com.aricneto.twistytimer.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class TwistyColors(
    val backgroundGradientStart: Color,
    val backgroundGradientEnd: Color,
    val isLight: Boolean = false
)

val LocalTwistyColors = staticCompositionLocalOf {
    TwistyColors(
        backgroundGradientStart = Color(0xFF1959FF),
        backgroundGradientEnd = Color(0xFF3B12FF)
    )
}

// Helper to create ColorScheme from legacy attributes
private fun createDarkColorScheme(
    primary: Color = Color.White,
    secondary: Color,
    tertiary: Color,
    surface: Color = Color(0xFF121212),
    surfaceContainer: Color = Color(0xFF1F1F1F)
) = darkColorScheme(
    primary = primary,
    secondary = secondary,
    tertiary = tertiary,
    background = surface,
    surface = surface,
    surfaceVariant = surfaceContainer,
    onPrimary = if (primary == Color.White) Color.Black else Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF444444)
)

private fun createLightColorScheme(
    primary: Color = Color.Black,
    secondary: Color,
    tertiary: Color,
    surface: Color = Color(0xFFEEEEEE),
    surfaceContainer: Color = Color.White
) = lightColorScheme(
    primary = primary,
    secondary = secondary,
    tertiary = tertiary,
    background = surface,
    surface = surface,
    surfaceVariant = surfaceContainer,
    onPrimary = if (primary == Color.Black) Color.White else Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black.copy(alpha = 0.7f),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFE0E0E0)
)

// Legacy Theme Definitions
private val IndigoTheme = createDarkColorScheme(secondary = Color(0xFF1959FF), tertiary = Color(0xFF2962FF))
private val IndigoTwisty = TwistyColors(Color(0xFF1959FF), Color(0xFF3B12FF))

private val PinkTheme = createDarkColorScheme(secondary = Color(0xFFFF577E), tertiary = Color(0xFFFD61C0), surface = Color(0xFF1F1216), surfaceContainer = Color(0xFF2D1A20))
private val PinkTwisty = TwistyColors(Color(0xFFFF577E), Color(0xFFFD61C0))

private val PurpleTheme = createDarkColorScheme(secondary = Color(0xFF673AB7), tertiary = Color(0xFF512DA8), surface = Color(0xFF16121F), surfaceContainer = Color(0xFF201A2D))
private val PurpleTwisty = TwistyColors(Color(0xFF673AB7), Color(0xFF512DA8))

private val BrownTheme = createDarkColorScheme(secondary = Color(0xFF5C3C30), tertiary = Color(0xFFFFD740), surface = Color(0xFF1F1A12), surfaceContainer = Color(0xFF2D251A))
private val BrownTwisty = TwistyColors(Color(0xFF5C3C30), Color(0xFF422A24))

private val BlueTheme = createDarkColorScheme(secondary = Color(0xFF920EE6), tertiary = Color(0xFFFFC107), surface = Color(0xFF12161F), surfaceContainer = Color(0xFF1A202D))
private val BlueTwisty = TwistyColors(Color(0xFF920EE6), Color(0xFF6304E9))

private val OrangeTheme = createDarkColorScheme(secondary = Color(0xFFED1C24), tertiary = Color(0xFF19C2FF), surface = Color(0xFF1F1412), surfaceContainer = Color(0xFF2D1D1A))
private val OrangeTwisty = TwistyColors(Color(0xFFED1C24), Color(0xFFFF6219))

private val DeepPurpleTheme = createDarkColorScheme(secondary = Color(0xFFED1E79), tertiary = Color(0xFFFFC400), surface = Color(0xFF18121F), surfaceContainer = Color(0xFF221A2D))
private val DeepPurpleTwisty = TwistyColors(Color(0xFFED1E79), Color(0xFF662D8C))

private val BluyGrayTheme = createDarkColorScheme(secondary = Color(0xFF607D8B), tertiary = Color(0xFFFFD740), surface = Color(0xFF12181F), surfaceContainer = Color(0xFF1A222D))
private val BluyGrayTwisty = TwistyColors(Color(0xFF607D8B), Color(0xFF455A64))

private val WanderingDuskTheme = createDarkColorScheme(secondary = Color(0xFF8E78FF), tertiary = Color(0xFF7BFC83), surface = Color(0xFF181218), surfaceContainer = Color(0xFF221A22))
private val WanderingDuskTwisty = TwistyColors(Color(0xFF8E78FF), Color(0xFFFC7D7B))

private val SpottyGuyTheme = createDarkColorScheme(secondary = Color(0xFF0D0D0D), tertiary = Color(0xFF1DB954), surface = Color(0xFF121212), surfaceContainer = Color(0xFF121212))
private val SpottyGuyTwisty = TwistyColors(Color(0xFF0D0D0D), Color(0xFF0D0D0D))

private val BlackTheme = createDarkColorScheme(primary = Color.White, secondary = Color.Black, tertiary = Color(0xFFFFD740), surface = Color.Black, surfaceContainer = Color(0xFF121212))
private val BlackTwisty = TwistyColors(Color.Black, Color.Black)

// Light Themes
private val RedTheme = createLightColorScheme(secondary = Color(0xFFFBB74C), tertiary = Color(0xFFFF5656))
private val RedTwisty = TwistyColors(Color(0xFFFBB74C), Color(0xFFFF5656), isLight = true)

private val LightBlueTheme = createLightColorScheme(secondary = Color(0xFF3AFAFA), tertiary = Color(0xFFF76642))
private val LightBlueTwisty = TwistyColors(Color(0xFF3AFAFA), Color(0xFF42F7CD), isLight = true)

private val LightGreenTheme = createLightColorScheme(secondary = Color(0xFFA8FF78), tertiary = Color(0xFFFF8F78))
private val LightGreenTwisty = TwistyColors(Color(0xFFA8FF78), Color(0xFF78FFD6), isLight = true)

private val CyanTheme = createLightColorScheme(secondary = Color(0xFF00FFA1), tertiary = Color(0xFFFF4000))
private val CyanTwisty = TwistyColors(Color(0xFF00FFA1), Color(0xFF00FFFF), isLight = true)

private val TealTheme = createLightColorScheme(secondary = Color(0xFF18FF97), tertiary = Color(0xFFF53F24))
private val TealTwisty = TwistyColors(Color(0xFF18FF97), Color(0xFF24F594), isLight = true)

private val GreenTheme = createLightColorScheme(secondary = Color(0xFF6CF63E), tertiary = Color(0xFFFF3415))
private val GreenTwisty = TwistyColors(Color(0xFF6CF63E), Color(0xFF15FF8E), isLight = true)

private val BlueGrayTheme = createLightColorScheme(secondary = Color(0xFFFFFEFF), tertiary = Color(0xFFFF5252))
private val BlueGrayTwisty = TwistyColors(Color(0xFFFFFEFF), Color(0xFFD7FFFE), isLight = true)

private val WhiteTheme = createLightColorScheme(primary = Color.Black, secondary = Color.White, tertiary = Color(0xFFFF5252), surface = Color.White, surfaceContainer = Color(0xFFF5F5F5))
private val WhiteTwisty = TwistyColors(Color.White, Color.White, isLight = true)

private val WhiteGreenTheme = createLightColorScheme(secondary = Color(0xFFC6FFBD), tertiary = Color(0xFFFF5252))
private val WhiteGreenTwisty = TwistyColors(Color(0xFFC6FFBD), Color.White, isLight = true)

private val YellowTheme = createLightColorScheme(secondary = Color(0xFFFFEB7E), tertiary = Color(0xFFFFFD69))
private val YellowTwisty = TwistyColors(Color(0xFFFFEB7E), Color(0xFFFFFD69), isLight = true)

private val DawnTheme = createLightColorScheme(secondary = Color(0xFFFCCB90), tertiary = Color(0xFFEBE07E))
private val DawnTwisty = TwistyColors(Color(0xFFFCCB90), Color(0xFFD57EEB), isLight = true)

private val TurtlySeaTheme = createLightColorScheme(secondary = Color(0xFFB2F9FF), tertiary = Color(0xFFFF5252))
private val TurtlySeaTwisty = TwistyColors(Color(0xFFB2F9FF), Color(0xFFEFEBBE), isLight = true)

private val PixieFallsTheme = createLightColorScheme(secondary = Color(0xFFFCD6E3), tertiary = Color(0xFFFF5252))
private val PixieFallsTwisty = TwistyColors(Color(0xFFFCD6E3), Color(0xFFAAF0ED), isLight = true)

@Composable
fun TwistyTheme(
    themeName: String = "indigo",
    textStyle: String = "default",
    content: @Composable () -> Unit
) {
    val (colorScheme, twistyColors) = when (themeName) {
        "pink" -> PinkTheme to PinkTwisty
        "purple" -> PurpleTheme to PurpleTwisty
        "brown" -> BrownTheme to BrownTwisty
        "blue" -> BlueTheme to BlueTwisty
        "orange" -> OrangeTheme to OrangeTwisty
        "deeppurple" -> DeepPurpleTheme to DeepPurpleTwisty
        "bluy_gray" -> BluyGrayTheme to BluyGrayTwisty
        "wandering_dusk" -> WanderingDuskTheme to WanderingDuskTwisty
        "spotty_guy" -> SpottyGuyTheme to SpottyGuyTwisty
        "black" -> BlackTheme to BlackTwisty
        "red" -> RedTheme to RedTwisty
        "light_blue" -> LightBlueTheme to LightBlueTwisty
        "light_green" -> LightGreenTheme to LightGreenTwisty
        "cyan" -> CyanTheme to CyanTwisty
        "teal" -> TealTheme to TealTwisty
        "green" -> GreenTheme to GreenTwisty
        "bluegray" -> BlueGrayTheme to BlueGrayTwisty
        "white" -> WhiteTheme to WhiteTwisty
        "white_green" -> WhiteGreenTheme to WhiteGreenTwisty
        "yellow" -> YellowTheme to YellowTwisty
        "dawn" -> DawnTheme to DawnTwisty
        "turtly_sea" -> TurtlySeaTheme to TurtlySeaTwisty
        "pixie_falls" -> PixieFallsTheme to PixieFallsTwisty
        else -> IndigoTheme to IndigoTwisty
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = twistyColors.isLight
        }
    }

    val typography = getTypographyForStyle(textStyle)

    CompositionLocalProvider(LocalTwistyColors provides twistyColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
