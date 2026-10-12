package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = RetailRedPrimary,
    onPrimary = Color.White,
    primaryContainer = RetailRedLight,
    onPrimaryContainer = RetailRedDark,
    secondary = RetailNavy,
    onSecondary = Color.White,
    secondaryContainer = RetailBlueLight,
    onSecondaryContainer = RetailNavy,
    tertiary = RetailAmber,
    onTertiary = Color.White,
    tertiaryContainer = RetailAmberLight,
    background = Color(0xFFF6F8FA),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color.Black,
    error = Color(0xFFC62828),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEF5350),
    onPrimary = Color.White,
    primaryContainer = RetailRedDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF64B5F6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1565C0),
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color.White
)

@Composable
fun DodolanProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
