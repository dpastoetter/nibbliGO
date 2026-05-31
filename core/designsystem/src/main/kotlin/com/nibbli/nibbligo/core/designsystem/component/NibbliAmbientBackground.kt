package com.nibbli.nibbligo.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import com.nibbli.nibbligo.core.designsystem.theme.LocalAccentGlow
import com.nibbli.nibbligo.core.designsystem.theme.SuperDarkGlowLavender
import com.nibbli.nibbligo.core.designsystem.theme.SuperDarkGlowTeal

@Composable
fun NibbliAmbientBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val isSuperDark = scheme.background.luminance() < 0.04f
    val accentGlow = LocalAccentGlow.current
    val centerY = 0.32f
    val base = scheme.background

    val edge = when {
        isSuperDark -> SuperDarkGlowTeal.copy(alpha = 0.55f)
        isDark -> accentGlow.copy(alpha = 0.35f)
        else -> accentGlow.copy(alpha = 0.5f)
    }
    val mid = when {
        isSuperDark -> SuperDarkGlowLavender.copy(alpha = 0.22f)
        else -> base
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = if (isSuperDark) {
                    listOf(
                        accentGlow.copy(alpha = 0.35f),
                        mid,
                        base,
                        edge,
                    )
                } else {
                    listOf(
                        accentGlow.copy(alpha = if (isDark) 0.28f else 0.12f),
                        base,
                        edge.copy(alpha = if (isDark) 0.25f else 0.08f),
                    )
                },
                center = Offset(size.width * 0.5f, size.height * centerY),
                radius = size.maxDimension * 0.9f,
            ),
        )
    }
}
