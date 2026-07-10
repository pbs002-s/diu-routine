package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BentoPrimaryDark,
    secondary = BentoSecondaryDark,
    tertiary = BentoTertiaryDark,
    background = BentoBackgroundDark,
    surface = BentoSurfaceDark,
    onPrimary = Color(0xFF003825),
    onSecondary = Color(0xFF233529),
    onTertiary = Color(0xFF381E72),
    onBackground = Color(0xFFE1E3E1),
    onSurface = Color(0xFFE1E3E1),
    outline = BentoNormalCardBorderDark,
    outlineVariant = BentoNormalCardBorderDark,
    surfaceVariant = BentoPillBgDark,
    primaryContainer = BentoGreenCardBgDark,
    secondaryContainer = BentoPurpleCardBgDark,
    onPrimaryContainer = Color(0xFF81D4B4),
    onSecondaryContainer = Color(0xFFE8DEF8)
)

private val LightColorScheme = lightColorScheme(
    primary = BentoPrimary,
    secondary = BentoSecondary,
    tertiary = BentoTertiary,
    background = BentoBackground,
    surface = BentoSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A1C1A),
    onSurface = Color(0xFF1A1C1A),
    outline = BentoNormalCardBorder,
    outlineVariant = BentoNormalCardBorder,
    surfaceVariant = BentoPillBg,
    primaryContainer = BentoGreenCardBg,
    secondaryContainer = BentoPurpleCardBg,
    onPrimaryContainer = BentoPrimary,
    onSecondaryContainer = BentoPurpleText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
