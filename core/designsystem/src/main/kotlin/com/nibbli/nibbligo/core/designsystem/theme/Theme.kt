package com.nibbli.nibbligo.core.designsystem.theme

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
import com.nibbli.nibbligo.core.model.AppThemeMode

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    secondary = LavenderAccent,
    onSecondary = Color.White,
    tertiary = WarmCoral,
    background = CreamSurface,
    onBackground = DeepSlate,
    surface = Color.White,
    onSurface = DeepSlate,
    surfaceVariant = Color(0xFFE8E4DE),
    onSurfaceVariant = Color(0xFF5A6570),
)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    secondary = LavenderAccent,
    onSecondary = Color.White,
    tertiary = WarmCoral,
    background = DeepSlate,
    onBackground = MistText,
    surface = SlateSurface,
    onSurface = MistText,
    surfaceVariant = SlateCard,
    onSurfaceVariant = MistText,
)

private val SuperDarkColorScheme = darkColorScheme(
    primary = SuperDarkPrimary,
    onPrimary = SuperDarkOnPrimary,
    primaryContainer = SuperDarkPrimaryContainer,
    onPrimaryContainer = SuperDarkOnPrimaryContainer,
    secondary = SuperDarkSecondary,
    onSecondary = Color(0xFF1E1830),
    secondaryContainer = SuperDarkSecondaryContainer,
    onSecondaryContainer = Color(0xFFD4C8F0),
    tertiary = SuperDarkTertiary,
    onTertiary = Color(0xFF2A1810),
    tertiaryContainer = SuperDarkTertiaryContainer,
    onTertiaryContainer = Color(0xFFF0C8B8),
    background = SuperDarkBackground,
    onBackground = SuperDarkOnBackground,
    surface = SuperDarkSurface,
    onSurface = SuperDarkOnSurface,
    surfaceVariant = SuperDarkSurfaceElevated,
    onSurfaceVariant = SuperDarkMuted,
    surfaceContainerLowest = SuperDarkBackground,
    surfaceContainerLow = SuperDarkSurface,
    surfaceContainer = SuperDarkSurfaceElevated,
    surfaceContainerHigh = SuperDarkSurfaceHigh,
    surfaceContainerHighest = SuperDarkSurfaceBright,
    outline = SuperDarkOutline,
    outlineVariant = SuperDarkOutlineVariant,
    inverseSurface = SuperDarkOnSurface,
    inverseOnSurface = SuperDarkBackground,
)

@Composable
fun NibbliTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.SUPER_DARK -> SuperDarkColorScheme
        AppThemeMode.SYSTEM -> if (systemDark) DarkColorScheme else LightColorScheme
    }
    val useDarkStatusBarIcons = when (themeMode) {
        AppThemeMode.LIGHT -> true
        AppThemeMode.DARK, AppThemeMode.SUPER_DARK -> false
        AppThemeMode.SYSTEM -> !systemDark
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = useDarkStatusBarIcons
            controller.isAppearanceLightNavigationBars = useDarkStatusBarIcons
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NibbliTypography,
        content = content,
    )
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
