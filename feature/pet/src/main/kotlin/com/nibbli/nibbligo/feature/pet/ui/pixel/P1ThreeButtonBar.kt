package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun P1ThreeButtonBar(
    onButtonLeft: () -> Unit,
    onButtonCenter: () -> Unit,
    onButtonRight: () -> Unit,
    cycleEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
    highlightConfirm: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        P1RecessedButton(
            label = "◀",
            contentDescription = "Previous care action",
            onClick = onButtonLeft,
            enabled = cycleEnabled,
        )
        P1RecessedButton(
            label = "●",
            contentDescription = "Confirm care action",
            onClick = onButtonCenter,
            enabled = confirmEnabled,
            wide = true,
            highlight = highlightConfirm,
        )
        P1RecessedButton(
            label = "▶",
            contentDescription = "Next care action",
            onClick = onButtonRight,
            enabled = cycleEnabled,
        )
    }
}

@Composable
private fun P1RecessedButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    wide: Boolean = false,
    highlight: Boolean = false,
) {
    val width = if (wide) 72.dp else P1Theme.ButtonWidth
    val colors = p1Colors()
    val pulseAlpha by rememberInfiniteTransition(label = "confirmPulse").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "confirmPulseAlpha",
    )
    val highlightAlpha = if (highlight && enabled) pulseAlpha else 1f
    Box(
        modifier = Modifier
            .width(width)
            .height(P1Theme.ButtonHeight)
            .alpha(highlightAlpha)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) colors.buttonRecess else colors.buttonRecess.copy(alpha = 0.5f))
            .border(
                width = if (highlight && enabled) 2.dp else 1.dp,
                color = if (highlight && enabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                } else {
                    colors.buttonRecessDark
                },
                shape = RoundedCornerShape(50),
            )
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) colors.buttonIcon else colors.buttonIcon.copy(alpha = 0.4f),
        )
    }
}
