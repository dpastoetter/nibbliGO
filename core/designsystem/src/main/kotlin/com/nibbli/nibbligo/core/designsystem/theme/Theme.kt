package com.nibbli.nibbligo.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun NibbliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NibbliTypography,
        content = content,
    )
}
