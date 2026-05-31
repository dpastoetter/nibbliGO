package com.nibbli.nibbligo.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nibbli.nibbligo.core.model.AppAccentPalette
import com.nibbli.nibbligo.core.model.AppThemeMode

val LocalAccentGlow = staticCompositionLocalOf { AccentColors.tokens(AppAccentPalette.TEAL, ThemeBase.LIGHT).glow }

@Composable
fun NibbliTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    accentPalette: AppAccentPalette = AppAccentPalette.TEAL,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = AccentColors.colorScheme(themeMode, accentPalette, systemDark)
    val base = when (themeMode) {
        AppThemeMode.SUPER_DARK -> ThemeBase.SUPER_DARK
        AppThemeMode.DARK -> ThemeBase.DARK
        AppThemeMode.LIGHT -> ThemeBase.LIGHT
        AppThemeMode.SYSTEM -> if (systemDark) ThemeBase.DARK else ThemeBase.LIGHT
    }
    val accentGlow = AccentColors.tokens(accentPalette, base).glow
    val useDarkStatusBarIcons = when (themeMode) {
        AppThemeMode.LIGHT -> true
        AppThemeMode.DARK, AppThemeMode.SUPER_DARK -> false
        AppThemeMode.SYSTEM -> !systemDark
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = useDarkStatusBarIcons
            controller.isAppearanceLightNavigationBars = useDarkStatusBarIcons
        }
    }
    CompositionLocalProvider(LocalAccentGlow provides accentGlow) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NibbliTypography,
            content = content,
        )
    }
}

/** @deprecated Use [themeMode] — kept for previews that pass [darkTheme] only. */
@Composable
fun NibbliTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val mode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT
    NibbliTheme(themeMode = mode, content = content)
}
