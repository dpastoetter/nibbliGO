package com.nibbli.nibbligo.feature.agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.nibbli.nibbligo.core.designsystem.component.ConfirmActionDialog
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.OnDeviceBadge
import com.nibbli.nibbligo.core.model.AgentMessageRole
import com.nibbli.nibbligo.core.ui.EmptyState
import com.nibbli.nibbligo.feature.agent.presentation.AgentChatViewModel

@Composable
fun AgentChatScreen(
    modifier: Modifier = Modifier,
    viewModel: AgentChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.installedModelIds.isEmpty()) {
        EmptyState(
            title = "Install a model first",
            subtitle = "Agent mode needs an installed LiteRT model (e.g. functiongemma-270m or gemma-4-e2b-it).",
            modifier = modifier,
        )
        return
    }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Agent Chat", style = MaterialTheme.typography.displaySmall)
        OnDeviceBadge(Modifier.padding(vertical = 8.dp))
        Text(
            "nibbli can propose tools — you approve sensitive steps.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        uiState.installedModelIds.forEach { id ->
            FilterChip(
                selected = uiState.session.modelId == id,
                onClick = { viewModel.selectModel(id) },
                label = { Text(id) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        if (uiState.supportsThinking) {
            FilterChip(
                selected = uiState.showThinkingTrace,
                onClick = { viewModel.toggleThinking() },
                label = { Text("Thinking trace") },
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.showThinkingTrace) {
                items(uiState.session.steps) { step ->
                    NibbliCard {
                        Text(
                            step.kind.name.replace('_', ' '),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(step.summary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            items(uiState.session.messages) { msg ->
                NibbliCard {
                    val who = when (msg.role) {
                        AgentMessageRole.USER -> "You"
                        AgentMessageRole.ASSISTANT -> "nibbli"
                        AgentMessageRole.TOOL -> "Tool"
                        AgentMessageRole.SYSTEM -> "System"
                    }
                    Text(who, style = MaterialTheme.typography.labelLarge)
                    Text(msg.content, modifier = Modifier.padding(top = 4.dp).testTag("agent_message"))
                }
            }
        }
        OutlinedTextField(
            value = uiState.input,
            onValueChange = viewModel::updateInput,
            modifier = Modifier.fillMaxWidth().testTag("agent_input"),
            label = { Text("Ask nibbli to help…") },
            placeholder = { Text("Try: remind me to test the agent") },
        )
        Button(
            onClick = { viewModel.send() },
            modifier = Modifier.padding(top = 8.dp).testTag("agent_send"),
            enabled = !uiState.session.isRunning,
        ) { Text("Send") }
        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
    if (uiState.pendingConfirmation != null) {
        ConfirmActionDialog(
            title = uiState.pendingToolTitle,
            description = "This tool runs on your device after you confirm.",
            preview = uiState.pendingToolPreview,
            onConfirm = { viewModel.confirmTool() },
            onDismiss = { viewModel.dismissTool() },
        )
    }
}
