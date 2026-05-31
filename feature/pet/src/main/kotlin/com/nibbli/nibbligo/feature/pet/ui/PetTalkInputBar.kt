package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val PetTalkInputShape = RoundedCornerShape(20.dp)
private val PetTalkTrailingActionWidth = 52.dp

@Composable
fun PetTalkInputBar(
    enabled: Boolean,
    isPetAlive: Boolean,
    isUserTalkGenerating: Boolean,
    isVoiceListening: Boolean,
    onTalkToMeClick: () -> Unit,
    onSend: (String) -> Unit,
    onStopClick: () -> Unit = {},
    micEnabled: Boolean = enabled,
    isWarmingModel: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val inputEnabled = enabled && !isWarmingModel
    val canSend = inputEnabled && text.isNotBlank() && !isVoiceListening
    val placeholder = when {
        !isPetAlive -> "Hatch a new egg to talk again…"
        isWarmingModel -> "Warming up model…"
        isUserTalkGenerating -> "nibbli is thinking…"
        isVoiceListening -> "Listening…"
        else -> "Message nibbli…"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isPetAlive) {
            PetTalkToMeButton(
                enabled = micEnabled,
                isListening = isVoiceListening,
                onClick = onTalkToMeClick,
                iconOnly = true,
            )
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(PetTalkActionHeight),
            shape = PetTalkInputShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                alpha = if (inputEnabled) 1f else 0.5f,
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
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    enabled = inputEnabled && !isVoiceListening,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
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
                if (isUserTalkGenerating) {
                    Box(
                        modifier = Modifier
                            .width(PetTalkTrailingActionWidth)
                            .height(PetTalkActionHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = onStopClick) {
                            Text("Stop", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(PetTalkTrailingActionWidth)
                            .height(PetTalkActionHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = {
                                val message = text.trim()
                                if (message.isEmpty() || !canSend) return@IconButton
                                text = ""
                                onSend(message)
                            },
                            enabled = canSend,
                            modifier = Modifier.size(36.dp),
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
    }
}
