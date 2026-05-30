package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PetTalkToMeButton(
    enabled: Boolean,
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val contentColor = when {
        isListening -> MaterialTheme.colorScheme.onPrimaryContainer
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    val talkDescription = if (isListening) "Listening" else "Talk to me"

    if (iconOnly) {
        val border = if (enabled || isListening) {
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        }
        Surface(
            onClick = onClick,
            enabled = enabled && !isListening,
            modifier = modifier
                .size(PetTalkActionHeight)
                .semantics { contentDescription = talkDescription },
            shape = RoundedCornerShape(20.dp),
            color = when {
                isListening -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            },
            border = border,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(if (isListening) pulseAlpha else 1f),
                    tint = contentColor,
                )
            }
        }
        return
    }

    PetTalkActionChip(
        label = if (isListening) "Listening…" else "Talk to me",
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = talkDescription },
        enabled = enabled && !isListening,
        emphasized = true,
        selected = isListening,
        leadingContent = {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .alpha(if (isListening) pulseAlpha else 1f),
                tint = contentColor,
            )
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor,
            )
        },
    )
}
