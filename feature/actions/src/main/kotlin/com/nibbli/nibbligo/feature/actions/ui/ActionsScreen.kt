package com.nibbli.nibbligo.feature.actions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.ConfirmActionDialog
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.feature.actions.presentation.ActionsViewModel

@Composable
fun ActionsScreen(
    modifier: Modifier = Modifier,
    viewModel: ActionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Actions", style = MaterialTheme.typography.displaySmall)
        FilterChip(
            selected = uiState.petTaskMode,
            onClick = { viewModel.togglePetTaskMode() },
            label = { Text("Pet task mode") },
        )
        uiState.actions.forEach { action ->
            NibbliCard {
                Text(action.title, style = MaterialTheme.typography.titleMedium)
                Text(action.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                Button(
                    onClick = { viewModel.requestAction(action.id) },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Run") }
            }
        }
        Text("MCP servers", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Add a StreamableHTTP MCP endpoint (see Gallery MCP guide). Tools require confirmation in Agent Chat.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = uiState.mcpServerUrlInput,
            onValueChange = viewModel::updateMcpUrlInput,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("MCP server URL") },
            placeholder = { Text("https://your-tunnel/mcp") },
        )
        Button(onClick = { viewModel.addMcpServer() }) { Text("Add MCP server") }
        uiState.mcpServers.forEach { server ->
            Text("• ${server.name}: ${server.url}", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Installed skill packages (${uiState.installedSkillPackages.size})",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        uiState.installedSkillPackages.forEach { pkg ->
            NibbliCard {
                Text(pkg.displayName, style = MaterialTheme.typography.titleMedium)
                Text(pkg.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                Text("v${pkg.version} · ${pkg.permissions}", style = MaterialTheme.typography.labelSmall)
                FilterChip(
                    selected = pkg.enabled,
                    onClick = { viewModel.toggleSkillEnabled(pkg.skillId, !pkg.enabled) },
                    label = { Text(if (pkg.enabled) "Enabled" else "Disabled") },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Text("Built-in skills", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        uiState.builtinSkills.forEach { skill ->
            NibbliCard {
                Text(skill.name, style = MaterialTheme.typography.titleMedium)
                Text(skill.description, modifier = Modifier.padding(top = 4.dp))
                Text(skill.permissionRationale, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                OutlinedButton(
                    onClick = { viewModel.requestSkill(skill.id) },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Use skill") }
            }
        }
        uiState.resultMessage?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        }
    }

    uiState.pendingDraft?.let { draft ->
        ConfirmActionDialog(
            title = draft.title,
            description = draft.description,
            preview = draft.preview,
            onConfirm = { viewModel.confirmPending() },
            onDismiss = { viewModel.dismissPending() },
        )
    }
    uiState.pendingSkillSummary?.let { summary ->
        ConfirmActionDialog(
            title = "Approve skill",
            description = "This skill runs only after you confirm.",
            preview = summary,
            onConfirm = { viewModel.confirmPending() },
            onDismiss = { viewModel.dismissPending() },
        )
    }
}
