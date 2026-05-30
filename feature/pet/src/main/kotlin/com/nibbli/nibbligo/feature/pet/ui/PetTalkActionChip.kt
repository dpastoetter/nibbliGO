package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

internal val PetTalkActionHeight = 40.dp
private val PetTalkActionShape = RoundedCornerShape(20.dp)

@Composable
internal fun PetTalkActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
    selected: Boolean = false,
    leadingContent: @Composable RowScope.() -> Unit = {},
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val border = when {
        emphasized && enabled -> BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                ),
            ),
        )
        else -> BorderStroke(1.dp, outline)
    }
    val containerColor = when {
        selected && enabled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        emphasized && enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
    }
    val contentColor = when {
        selected && enabled -> MaterialTheme.colorScheme.onPrimaryContainer
        emphasized && enabled -> MaterialTheme.colorScheme.primary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(PetTalkActionHeight),
        shape = PetTalkActionShape,
        color = containerColor,
        border = border,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent()
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
            )
        }
    }
}
