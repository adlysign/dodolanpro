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

import androidx.compose.material3.OutlinedTextFieldDefaults

private val RetailLightColorScheme = lightColorScheme(
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
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFC62828),
    onError = Color.White
)

@Composable
fun retailTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF0F172A),
    disabledTextColor = Color(0xFF64748B),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = RetailNavy,
    focusedBorderColor = RetailNavy,
    unfocusedBorderColor = Color(0xFF94A3B8),
    focusedLabelColor = RetailNavy,
    unfocusedLabelColor = Color(0xFF334155),
    focusedPlaceholderColor = Color(0xFF64748B),
    unfocusedPlaceholderColor = Color(0xFF64748B),
    focusedLeadingIconColor = RetailNavy,
    unfocusedLeadingIconColor = Color(0xFF475569),
    focusedTrailingIconColor = RetailNavy,
    unfocusedTrailingIconColor = Color(0xFF475569),
    focusedPrefixColor = Color(0xFF0F172A),
    unfocusedPrefixColor = Color(0xFF334155),
    focusedSuffixColor = Color(0xFF0F172A),
    unfocusedSuffixColor = Color(0xFF334155)
)

@Composable
fun DodolanProTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Selalu gunakan color scheme dengan kontras tinggi agar teks di seluruh form dan kartu terlihat jelas
    val colorScheme = RetailLightColorScheme
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
