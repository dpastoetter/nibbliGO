package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Immutable
data class P1Colors(
    val shellBody: Color,
    val shellBorder: Color,
    val shellShadow: Color,
    val lcdWell: Color,
    val lcdGreen: Color,
    val lcdGreenDark: Color,
    val lcdPixel: Color,
    val buttonRecess: Color,
    val buttonRecessDark: Color,
    val footerText: Color,
)

object P1Theme {
    val ShellRadius = 8.dp
    val ShellPadding = 10.dp
    val LcdMargin = 8.dp
    val ButtonWidth = 56.dp
    val ButtonHeight = 28.dp

    val Light = P1Colors(
        shellBody = Color(0xFFF0E6DA),
        shellBorder = Color(0xFFD4C4B0),
        shellShadow = Color(0xFF8A7A6A),
        lcdWell = Color(0xFF3A3A3E),
        lcdGreen = Color(0xFF9EAD86),
        lcdGreenDark = Color(0xFF4A5D3A),
        lcdPixel = Color(0xFF1A1A1E),
        buttonRecess = Color(0xFFD9CBB8),
        buttonRecessDark = Color(0xFFB8A898),
        footerText = Color(0xFF6B5E52),
    )

    val Dark = P1Colors(
        shellBody = Color(0xFF2A3038),
        shellBorder = Color(0xFF1A1F26),
        shellShadow = Color(0xFFCBD5E0),
        lcdWell = Color(0xFF121418),
        lcdGreen = Color(0xFF6B7F58),
        lcdGreenDark = Color(0xFF3A4A2E),
        lcdPixel = Color(0xFF1A1A1E),
        buttonRecess = Color(0xFF3E4550),
        buttonRecessDark = Color(0xFF252A31),
        footerText = Color(0xFF9AA5B0),
    )
}

@Composable
fun p1Colors(): P1Colors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) P1Theme.Dark else P1Theme.Light
}
