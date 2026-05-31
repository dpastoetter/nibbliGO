package com.nibbli.nibbligo.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class NibbliMessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL,
}

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
    val resolvedBubbleColor = if (isUser && bubbleColor == MaterialTheme.colorScheme.surfaceContainerHigh) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        bubbleColor
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 4.dp,
            bottomEnd = 20.dp,
            bottomStart = 20.dp,
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 20.dp,
            bottomEnd = 20.dp,
            bottomStart = 20.dp,
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = bubbleShape,
                color = resolvedBubbleColor,
                border = BorderStroke(1.dp, borderColor),
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isUser) {
                Canvas(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .height(10.dp)
                        .fillMaxWidth(0.4f)
                        .align(Alignment.End),
                ) {
                    val path = Path().apply {
                        moveTo(size.width, 0f)
                        lineTo(size.width * 0.65f, 0f)
                        lineTo(size.width * 0.85f, size.height)
                        close()
                    }
                    drawPath(path, resolvedBubbleColor)
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .height(10.dp)
                        .fillMaxWidth(0.4f),
                ) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width * 0.35f, 0f)
                        lineTo(size.width * 0.15f, size.height)
                        close()
                    }
                    drawPath(path, resolvedBubbleColor)
                }
            }
        }
    }
}
