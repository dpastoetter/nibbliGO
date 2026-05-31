package com.nibbli.nibbligo.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val InlineChatInputShape = RoundedCornerShape(20.dp)
private val InlineChatInputHeight = 40.dp

@Composable
fun NibbliInlineChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Message…",
    isGenerating: Boolean = false,
    onStop: () -> Unit = {},
    inputTestTag: String? = null,
    sendTestTag: String? = null,
) {
    val canSend = enabled && value.isNotBlank() && !isGenerating

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(InlineChatInputHeight),
        shape = InlineChatInputShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = if (enabled) 1f else 0.5f,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .then(if (inputTestTag != null) Modifier.testTag(inputTestTag) else Modifier),
                enabled = enabled && !isGenerating,
                singleLine = true,
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (isGenerating) {
                TextButton(
                    onClick = onStop,
                    modifier = Modifier.widthIn(min = 48.dp),
                ) {
                    Text("Stop", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                IconButton(
                    onClick = {
                        val message = value.trim()
                        if (message.isEmpty() || !canSend) return@IconButton
                        onValueChange("")
                        onSend(message)
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(36.dp)
                        .then(if (sendTestTag != null) Modifier.testTag(sendTestTag) else Modifier),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(18.dp),
                        tint = if (canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                    )
                }
            }
        }
    }
}
