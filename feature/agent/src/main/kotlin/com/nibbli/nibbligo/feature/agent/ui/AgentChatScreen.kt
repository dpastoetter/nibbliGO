package com.nibbli.nibbligo.feature.agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import com.nibbli.nibbligo.core.designsystem.component.NibbliComposerStrip
import com.nibbli.nibbligo.core.designsystem.component.NibbliMessageBubble
import com.nibbli.nibbligo.core.designsystem.component.NibbliMessageRole
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.designsystem.component.NibbliTextField
import com.nibbli.nibbligo.core.model.AgentMessageRole
import com.nibbli.nibbligo.core.ui.EmptyState
import com.nibbli.nibbligo.feature.agent.presentation.AgentChatViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentChatScreen(
    modifier: Modifier = Modifier,
    viewModel: AgentChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.installedModelIds.isEmpty()) {
        NibbliScreen(modifier = modifier) {
            NibbliScreenHeader(
                title = "Agent Chat",
                showOnDeviceBadge = true,
            )
            EmptyState(
                title = "Install a model first",
                subtitle = "Agent mode needs FunctionGemma 270M for email and calendar drafts.",
            )
        }
        return
    }

    NibbliScreen(modifier = modifier) {
        NibbliScreenHeader(
            title = "Agent Chat",
            subtitle = "Email and calendar drafts via FunctionGemma — you confirm before anything runs.",
            showOnDeviceBadge = true,
        )

        if (uiState.agentModelMissing) {
            Text(
                "Install FunctionGemma 270M under Manage → Models for agent actions. " +
                    "It requires Hugging Face sign-in and accepting the model license.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        } else if (!uiState.supportsToolCalling && uiState.session.modelId.isNotBlank()) {
            Text(
                "Selected model cannot call phone tools. Switch to FunctionGemma 270M.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                val role = when (msg.role) {
                    AgentMessageRole.USER -> NibbliMessageRole.USER
                    AgentMessageRole.ASSISTANT -> NibbliMessageRole.ASSISTANT
                    AgentMessageRole.TOOL -> NibbliMessageRole.TOOL
                    AgentMessageRole.SYSTEM -> NibbliMessageRole.SYSTEM
                }
                val who = when (msg.role) {
                    AgentMessageRole.USER -> "You"
                    AgentMessageRole.ASSISTANT -> "nibbli"
                    AgentMessageRole.TOOL -> "Tool"
                    AgentMessageRole.SYSTEM -> "System"
                }
                NibbliMessageBubble(
                    text = msg.content,
                    role = role,
                    label = who,
                    modifier = Modifier.testTag("agent_message"),
                )
            }
        }

        NibbliComposerStrip {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NibbliSuggestionChip(
                    label = "Email draft",
                    selected = false,
                    onClick = {
                        viewModel.applyQuickPrompt(
                            "Draft an email to me about lunch tomorrow at noon",
                        )
                    },
                )
                NibbliSuggestionChip(
                    label = "Calendar draft",
                    selected = false,
                    onClick = {
                        viewModel.applyQuickPrompt(
                            "Create a calendar event tomorrow at 3pm titled Team sync",
                        )
                    },
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.installedModelIds.forEach { id ->
                    NibbliSuggestionChip(
                        label = viewModel.modelLabel(id),
                        selected = uiState.session.modelId == id,
                        onClick = { viewModel.selectModel(id) },
                    )
                }
                if (uiState.supportsThinking) {
                    NibbliSuggestionChip(
                        label = "Thinking trace",
                        selected = uiState.showThinkingTrace,
                        onClick = { viewModel.toggleThinking() },
                    )
                }
            }
            NibbliTextField(
                value = uiState.input,
                onValueChange = viewModel::updateInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("agent_input"),
                label = { Text("Ask nibbli to help…") },
                placeholder = { Text("Try: send an email to me about lunch") },
            )
            NibbliPrimaryButton(
                text = if (uiState.session.isRunning) "Thinking…" else "Send",
                onClick = { viewModel.send() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("agent_send"),
                enabled = !uiState.session.isRunning,
            )
            uiState.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                )
            }
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
