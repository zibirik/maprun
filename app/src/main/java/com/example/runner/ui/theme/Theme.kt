package com.example.runner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.runner.domain.model.AppThemeMode

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealSoft,
    onPrimaryContainer = TealDeep,
    secondary = Ember,
    onSecondary = Color.White,
    secondaryContainer = Sand,
    onSecondaryContainer = EmberDark,
    tertiary = GhostPurple,
    background = Fog,
    onBackground = Ink,
    surface = FogCard,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Color(0xFF3D5854),
    outline = Color(0xFFB7C9C5),
    error = Danger,
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Night,
    primaryContainer = TealDeep,
    onPrimaryContainer = TealSoft,
    secondary = Ember,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A220C),
    onSecondaryContainer = Sand,
    tertiary = GhostPurple,
    background = Night,
    onBackground = Color(0xFFE6F4F1),
    surface = NightCard,
    onSurface = Color(0xFFE6F4F1),
    surfaceVariant = Color(0xFF1A3330),
    onSurfaceVariant = Color(0xFFB7D0CB),
    outline = Color(0xFF3E5B56),
    error = Danger,
)

@Composable
fun MapRunTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = isAppDark(themeMode)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        shapes = MapRunShapes,
        content = content,
    )
}

@Composable
fun isAppDark(themeMode: AppThemeMode): Boolean {
    return when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
}
