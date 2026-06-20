package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.theme.OnDeviceGreen

@Immutable
data class P1Colors(
    val shellBody: Color,
    val shellBorder: Color,
    val lcdWell: Color,
    val lcdGreen: Color,
    val lcdGreenDark: Color,
    val lcdPixel: Color,
    val buttonRecess: Color,
    val buttonRecessDark: Color,
    val buttonIcon: Color,
    val footerText: Color,
)

object P1Theme {
    /** Dark pixels drawn on the green LCD (theme-independent). */
    val LcdInk = Color(0xFF1A1A1E)

    /** Matches [com.nibbli.nibbligo.core.designsystem.component.NibbliCard]. */
    val ShellRadius = 24.dp
    val LcdWellRadius = 12.dp
    val LcdMargin = 6.dp
    val ButtonWidth = 56.dp
    val ButtonHeight = 48.dp
}

@Composable
fun p1Colors(): P1Colors {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val lcdAlpha = if (isDark) 0.38f else 0.48f
    val lcdFlashAlpha = if (isDark) 0.52f else 0.62f
    return P1Colors(
        shellBody = scheme.surfaceContainerLow,
        shellBorder = scheme.outlineVariant.copy(alpha = 0.35f),
        lcdWell = scheme.surfaceContainer,
        lcdGreen = OnDeviceGreen.copy(alpha = lcdAlpha),
        lcdGreenDark = OnDeviceGreen.copy(alpha = lcdFlashAlpha),
        lcdPixel = P1Theme.LcdInk,
        buttonRecess = scheme.surfaceContainerHigh,
        buttonRecessDark = scheme.outlineVariant.copy(alpha = 0.45f),
        buttonIcon = scheme.onSurfaceVariant,
        footerText = scheme.onSurfaceVariant,
    )
}
