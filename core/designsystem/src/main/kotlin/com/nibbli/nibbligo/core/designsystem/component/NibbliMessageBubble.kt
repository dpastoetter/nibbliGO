package com.nibbli.nibbligo.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class NibbliMessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL,
}

private val BubbleMaxWidth = 300.dp
private val BubblePadding = 14.dp
private val BubbleCornerLarge = 20.dp
private val BubbleCornerSmall = 8.dp

@Composable
fun NibbliMessageBubble(
    text: String,
    role: NibbliMessageRole,
    modifier: Modifier = Modifier,
    label: String? = null,
    maxLines: Int = Int.MAX_VALUE,
    bubbleColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val isUser = role == NibbliMessageRole.USER
    val isSystem = role == NibbliMessageRole.SYSTEM || role == NibbliMessageRole.TOOL

    val colors = when {
        isUser -> BubbleColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            label = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            border = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        )
        isSystem -> BubbleColors(
            container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            label = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        )
        bubbleColor != MaterialTheme.colorScheme.surfaceContainerHigh -> BubbleColors(
            container = bubbleColor,
            content = MaterialTheme.colorScheme.onSurface,
            label = MaterialTheme.colorScheme.onSurfaceVariant,
            border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        )
        else -> BubbleColors(
            container = MaterialTheme.colorScheme.surfaceContainerLow,
            content = MaterialTheme.colorScheme.onSurface,
            label = MaterialTheme.colorScheme.onSurfaceVariant,
            border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        )
    }

    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = BubbleCornerLarge,
            topEnd = BubbleCornerLarge,
            bottomEnd = BubbleCornerSmall,
            bottomStart = BubbleCornerLarge,
        )
    } else {
        RoundedCornerShape(
            topStart = BubbleCornerLarge,
            topEnd = BubbleCornerLarge,
            bottomEnd = BubbleCornerLarge,
            bottomStart = BubbleCornerSmall,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = BubbleMaxWidth),
            shape = bubbleShape,
            color = colors.container,
            border = BorderStroke(1.dp, colors.border),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(BubblePadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.label,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.content,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class BubbleColors(
    val container: Color,
    val content: Color,
    val label: Color,
    val border: Color,
)
