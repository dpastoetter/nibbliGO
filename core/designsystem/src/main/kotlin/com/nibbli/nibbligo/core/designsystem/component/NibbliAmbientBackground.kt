package com.nibbli.nibbligo.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import com.nibbli.nibbligo.core.designsystem.theme.SuperDarkGlowLavender
import com.nibbli.nibbligo.core.designsystem.theme.SuperDarkGlowTeal

@Composable
fun NibbliAmbientBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val isSuperDark = scheme.background.luminance() < 0.04f
    val centerY = 0.32f
    val base = scheme.background

    val edge = when {
        isSuperDark -> SuperDarkGlowTeal.copy(alpha = 0.55f)
        isDark -> scheme.surface.copy(alpha = 0.35f)
        else -> scheme.primary.copy(alpha = 0.06f)
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
                        SuperDarkGlowTeal.copy(alpha = 0.35f),
                        mid,
                        base,
                        edge,
                    )
                } else {
                    listOf(base, edge)
                },
                center = Offset(size.width * 0.5f, size.height * centerY),
                radius = size.maxDimension * 0.9f,
            ),
        )
    }
}
