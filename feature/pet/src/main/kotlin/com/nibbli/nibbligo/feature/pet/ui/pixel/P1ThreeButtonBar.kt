package com.nibbli.nibbligo.feature.pet.ui.pixel

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun P1ThreeButtonBar(
    onButtonLeft: () -> Unit,
    onButtonCenter: () -> Unit,
    onButtonRight: () -> Unit,
    cycleEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        P1RecessedButton(label = "◀", onClick = onButtonLeft, enabled = cycleEnabled)
        P1RecessedButton(label = "●", onClick = onButtonCenter, enabled = confirmEnabled, wide = true)
        P1RecessedButton(label = "▶", onClick = onButtonRight, enabled = cycleEnabled)
    }
}

@Composable
private fun P1RecessedButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    wide: Boolean = false,
) {
    val width = if (wide) 72.dp else P1Theme.ButtonWidth
    val colors = p1Colors()
    Box(
        modifier = Modifier
            .width(width)
            .height(P1Theme.ButtonHeight)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) colors.buttonRecess else colors.buttonRecess.copy(alpha = 0.5f))
            .border(1.dp, colors.buttonRecessDark, RoundedCornerShape(50))
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
