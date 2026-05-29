package com.nibbli.nibbligo.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliMessageBubble
import com.nibbli.nibbligo.core.designsystem.component.NibbliMessageRole
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.ui.EmptyState
import com.nibbli.nibbligo.feature.chat.presentation.ChatViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.installedModelIds.isEmpty()) {
        NibbliScreen(modifier = modifier) {
            NibbliScreenHeader(
                title = "Local Chat",
                showOnDeviceBadge = true,
            )
            EmptyState(
                title = "No models installed",
                subtitle = "Download FunctionGemma or Gemma from Manage → Models to start chatting.",
            )
        }
        return
    }

    NibbliScreen(modifier = modifier) {
        NibbliScreenHeader(
            title = "Local Chat",
            subtitle = "On-device conversation with an installed LiteRT model.",
            showOnDeviceBadge = true,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            uiState.installedModelIds.forEach { id ->
                NibbliSuggestionChip(
                    label = id,
                    selected = uiState.selectedModelId == id,
                    onClick = { viewModel.selectModel(id) },
                )
            }
        }

        NibbliPrimaryButton(
            text = "New conversation",
            onClick = { viewModel.newConversation() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_chat")
                .padding(bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.messages) { msg ->
                val role = when (msg.role) {
                    MessageRole.USER -> NibbliMessageRole.USER
                    MessageRole.ASSISTANT -> NibbliMessageRole.ASSISTANT
                    else -> NibbliMessageRole.SYSTEM
                }
                val label = if (msg.role == MessageRole.USER) "You" else "nibbli"
                NibbliMessageBubble(
                    text = buildString {
                        append(msg.content)
                        msg.notes?.let { append("\n\nNotes: $it") }
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
                        label = "nibbli",
                        modifier = Modifier.testTag("streaming_text"),
                    )
                }
            }
        }

        NibbliSuggestionChip(
            label = "Working notes",
            selected = uiState.showReasoning,
            onClick = { viewModel.toggleReasoning() },
            modifier = Modifier.padding(top = 8.dp),
        )
        if (uiState.showReasoning) {
            OutlinedTextField(
                value = uiState.reasoningNotes,
                onValueChange = viewModel::updateReasoningNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Optional notes (visible, not hidden reasoning)") },
            )
        }

        OutlinedTextField(
            value = uiState.input,
            onValueChange = viewModel::updateInput,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chat_input"),
            label = { Text("Message") },
        )
        NibbliPrimaryButton(
            text = if (uiState.isStreaming) "Sending…" else "Send",
            onClick = { viewModel.sendMessage() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("send_message"),
            enabled = !uiState.isStreaming,
        )

        uiState.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
