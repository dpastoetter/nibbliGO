package com.nibbli.nibbligo.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.OnDeviceBadge
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.ui.EmptyState
import com.nibbli.nibbligo.feature.chat.presentation.ChatViewModel

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Local Chat", style = MaterialTheme.typography.displaySmall)
        OnDeviceBadge(Modifier.padding(vertical = 8.dp))

        if (uiState.installedModelIds.isEmpty()) {
            EmptyState(
                title = "No models installed",
                subtitle = "Install nibbli Fast from Manage → Models to start chatting offline.",
            )
            return
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            uiState.installedModelIds.forEach { id ->
                FilterChip(
                    selected = uiState.selectedModelId == id,
                    onClick = { viewModel.selectModel(id) },
                    label = { Text(id) },
                )
            }
        }

        Button(onClick = { viewModel.newConversation() }, modifier = Modifier.testTag("new_chat")) {
            Text("New conversation")
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.messages) { msg ->
                val label = if (msg.role == MessageRole.USER) "You" else "nibbli"
                NibbliCard {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                    Text(msg.content, modifier = Modifier.padding(top = 4.dp))
                    msg.notes?.let {
                        Text("Notes: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            uiState.streamingText?.let { streaming ->
                item {
                    NibbliCard {
                        Text("nibbli", style = MaterialTheme.typography.labelLarge)
                        Text(streaming, modifier = Modifier.padding(top = 4.dp).testTag("streaming_text"))
                    }
                }
            }
        }

        FilterChip(
            selected = uiState.showReasoning,
            onClick = { viewModel.toggleReasoning() },
            label = { Text("Working notes") },
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
            modifier = Modifier.fillMaxWidth().testTag("chat_input"),
            label = { Text("Message") },
        )
        Button(
            onClick = { viewModel.sendMessage() },
            modifier = Modifier.padding(top = 8.dp).testTag("send_message"),
            enabled = !uiState.isStreaming,
        ) { Text("Send") }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
