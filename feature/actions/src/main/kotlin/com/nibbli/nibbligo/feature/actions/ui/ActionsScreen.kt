package com.nibbli.nibbligo.feature.actions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.ConfirmActionDialog
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.mobileactions.MapParams
import com.nibbli.nibbligo.core.model.SafeAction
import com.nibbli.nibbligo.feature.actions.domain.PhoneActionInput
import com.nibbli.nibbligo.feature.actions.presentation.ActionsViewModel

@Composable
fun ActionsScreen(
    modifier: Modifier = Modifier,
    viewModel: ActionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NibbliScreen(modifier = modifier, scrollable = true) {
        NibbliScreenHeader(
            title = "Do",
            subtitle = "Phone tasks from Google AI Edge Gallery — run on-device with confirmation.",
            showOnDeviceBadge = true,
        )
        NibbliSuggestionChip(
            label = "Pet task mode",
            selected = uiState.petTaskMode,
            onClick = { viewModel.togglePetTaskMode() },
        )
        Text("Phone tasks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
        uiState.phoneActions.forEach { action ->
            ActionCard(action = action, onRun = { viewModel.requestAction(action.id) })
        }
        Text("Productivity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        uiState.productivityActions.forEach { action ->
            ActionCard(action = action, onRun = { viewModel.requestAction(action.id) })
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
        NibbliPrimaryButton(
            text = "Add MCP server",
            onClick = { viewModel.addMcpServer() },
            modifier = Modifier.fillMaxWidth(),
        )
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
                NibbliSuggestionChip(
                    label = if (pkg.enabled) "Enabled" else "Disabled",
                    selected = pkg.enabled,
                    onClick = { viewModel.toggleSkillEnabled(pkg.skillId, !pkg.enabled) },
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

    uiState.paramActionId?.let { actionId ->
        PhoneActionParamsDialog(
            actionId = actionId,
            input = uiState.pendingPhoneInput,
            onInputChange = viewModel::updatePhoneInput,
            onConfirm = { viewModel.submitPhoneParams() },
            onDismiss = { viewModel.dismissPhoneParams() },
        )
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

@Composable
private fun ActionCard(action: SafeAction, onRun: () -> Unit) {
    NibbliCard {
        Text(action.title, style = MaterialTheme.typography.titleMedium)
        Text(action.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        NibbliPrimaryButton(
            text = "Run",
            onClick = onRun,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun PhoneActionParamsDialog(
    actionId: String,
    input: PhoneActionInput,
    onInputChange: (PhoneActionInput) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle(actionId)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (actionId) {
                    "phone_create_contact" -> {
                        OutlinedTextField(
                            value = input.contact.firstName,
                            onValueChange = {
                                onInputChange(input.copy(contact = input.contact.copy(firstName = it)))
                            },
                            label = { Text("First name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = input.contact.lastName,
                            onValueChange = {
                                onInputChange(input.copy(contact = input.contact.copy(lastName = it)))
                            },
                            label = { Text("Last name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = input.contact.phoneNumber,
                            onValueChange = {
                                onInputChange(input.copy(contact = input.contact.copy(phoneNumber = it)))
                            },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = input.contact.email,
                            onValueChange = {
                                onInputChange(input.copy(contact = input.contact.copy(email = it)))
                            },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    "phone_send_email" -> {
                        OutlinedTextField(
                            value = input.email.to,
                            onValueChange = {
                                onInputChange(input.copy(email = input.email.copy(to = it)))
                            },
                            label = { Text("To") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = input.email.subject,
                            onValueChange = {
                                onInputChange(input.copy(email = input.email.copy(subject = it)))
                            },
                            label = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = input.email.body,
                            onValueChange = {
                                onInputChange(input.copy(email = input.email.copy(body = it)))
                            },
                            label = { Text("Body") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    "phone_show_map" -> {
                        OutlinedTextField(
                            value = input.map.location,
                            onValueChange = {
                                onInputChange(input.copy(map = MapParams(location = it)))
                            },
                            label = { Text("Location") },
                            placeholder = { Text("San Francisco airport") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    "phone_create_calendar" -> {
                        OutlinedTextField(
                            value = input.calendar.title,
                            onValueChange = {
                                onInputChange(input.copy(calendar = input.calendar.copy(title = it)))
                            },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = input.calendar.datetime,
                            onValueChange = {
                                onInputChange(input.copy(calendar = input.calendar.copy(datetime = it)))
                            },
                            label = { Text("Date/time") },
                            placeholder = { Text("2026-05-28T14:30:00") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun dialogTitle(actionId: String): String = when (actionId) {
    "phone_create_contact" -> "Create contact"
    "phone_send_email" -> "Send email"
    "phone_show_map" -> "Show on map"
    "phone_create_calendar" -> "Calendar event"
    else -> "Phone task"
}
