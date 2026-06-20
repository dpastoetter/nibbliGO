package com.nibbli.nibbligo.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nibbli.nibbligo.core.designsystem.component.NibbliComposerStrip
import com.nibbli.nibbligo.core.designsystem.component.NibbliInlineChatInputBar
import com.nibbli.nibbligo.core.designsystem.component.NibbliAmbientBackground
import com.nibbli.nibbligo.core.designsystem.component.isKeyboardVisible
import com.nibbli.nibbligo.core.pet.llm.PetTalkSuggestions
import com.nibbli.nibbligo.feature.pet.ui.PetTalkSuggestionChips
import com.nibbli.nibbligo.core.designsystem.component.NibbliMessageBubble
import com.nibbli.nibbligo.core.designsystem.component.NibbliMessageRole
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.core.designsystem.component.NibbliSecondaryButton
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.pet.llm.PetReactionParser
import com.nibbli.nibbligo.core.ui.EmptyState
import com.nibbli.nibbligo.feature.chat.presentation.ChatViewModel

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }

    if (!uiState.hasPetModel) {
        NibbliScreen(modifier = modifier) {
            NibbliScreenHeader(
                title = "Chat",
                subtitle = "Talk with your Pixel Friend on-device.",
                showOnDeviceBadge = true,
            )
            EmptyState(
                title = "Pixel Friend model needed",
                subtitle = "Download Qwen or SmolLM under Manage → Models, then set it in Manage → Companion.",
            )
        }
        return
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear chat?") },
            text = { Text("This removes your Pixel Friend chat history on this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearChat()
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        NibbliAmbientBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                NibbliScreenHeader(
                    title = "Chat with ${uiState.petName}",
                    subtitle = "Same conversation as Home — on-device Pixel Friend.",
                    showOnDeviceBadge = true,
                )

                NibbliSecondaryButton(
                    text = "Clear chat",
                    onClick = { showClearConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                navController?.let {
                    NibbliSecondaryButton(
                        text = "Agent — email & calendar",
                        onClick = { it.navigate("agent") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }

                val listState = rememberLazyListState()
                val messageCount = uiState.messages.size + if (uiState.streamingText != null) 1 else 0
                LaunchedEffect(messageCount, uiState.streamingText) {
                    if (messageCount > 0) {
                        listState.animateScrollToItem(messageCount - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = uiState.messages,
                        key = { msg -> "${msg.id}_${msg.timestampMillis}_${msg.role}" },
                    ) { msg ->
                        val role = when (msg.role) {
                            MessageRole.USER -> NibbliMessageRole.USER
                            MessageRole.ASSISTANT -> NibbliMessageRole.ASSISTANT
                            else -> NibbliMessageRole.SYSTEM
                        }
                        val label = when (msg.role) {
                            MessageRole.USER -> "You"
                            MessageRole.ASSISTANT -> uiState.petName
                            else -> "System"
                        }
                        NibbliMessageBubble(
                            text = if (msg.role == MessageRole.ASSISTANT) {
                                PetReactionParser.parseTalk(msg.content, uiState.petName).dialogue
                            } else {
                                msg.content
                            },
                            role = role,
                            label = label,
                        )
                    }
                    uiState.streamingText?.let { streaming ->
                        item {
                            NibbliMessageBubble(
                                text = streaming,
                                role = NibbliMessageRole.ASSISTANT,
                                label = uiState.petName,
                                modifier = Modifier.testTag("streaming_text"),
                            )
                        }
                    }
                }
            }

            NibbliComposerStrip {
                val keyboardVisible = isKeyboardVisible()
                val awaitingReply = uiState.isStreaming || uiState.streamingText != null
                if (!keyboardVisible && !awaitingReply && uiState.messages.isEmpty()) {
                    PetTalkSuggestionChips(
                        chips = PetTalkSuggestions.starterChips,
                        enabled = true,
                        onChipClick = viewModel::sendMessage,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                NibbliInlineChatInputBar(
                    value = uiState.input,
                    onValueChange = viewModel::updateInput,
                    onSend = viewModel::sendMessage,
                    enabled = !uiState.isStreaming,
                    placeholder = if (uiState.isStreaming) {
                        "${uiState.petName} is thinking…"
                    } else {
                        "Message ${uiState.petName}…"
                    },
                    isGenerating = uiState.isStreaming,
                    inputTestTag = "chat_input",
                    sendTestTag = "send_message",
                )
                uiState.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
