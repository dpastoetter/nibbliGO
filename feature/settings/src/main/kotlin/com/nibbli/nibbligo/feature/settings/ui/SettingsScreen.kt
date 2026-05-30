package com.nibbli.nibbligo.feature.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.feature.settings.presentation.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.onHuggingFaceAuthResult(result.data)
    }
    NibbliScreen(modifier = modifier, scrollable = true) {
        NibbliScreenHeader(
            title = "Settings & Privacy",
            subtitle = "Local storage, models, and on-device AI preferences.",
            showOnDeviceBadge = true,
        )
        AppearanceCard(
            themeMode = uiState.themeMode,
            onThemeModeChange = viewModel::setThemeMode,
        )
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("Local-first promise", style = MaterialTheme.typography.titleMedium)
            Text(
                "nibbliGO processes AI on your device. No hidden cloud inference in this build. " +
                    "Your chats, recordings, and prompts stay in local storage until you export.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("Storage", style = MaterialTheme.typography.titleMedium)
            Text(uiState.storageSummary, modifier = Modifier.padding(top = 8.dp))
            Text("Installed models: ${uiState.installedCount}", modifier = Modifier.padding(top = 4.dp))
        }
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("Hugging Face", style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    uiState.hfSignedIn -> "Token saved — used for gated Hugging Face models."
                    uiState.hfConfigured ->
                        "Sign in with OAuth, or paste a read token below (create at huggingface.co/settings/tokens)."
                    else ->
                        "LiteRT models download without sign-in when public. For gated repos, paste a HF access token below, " +
                            "or set hf.oauth.clientId in local.properties and rebuild for OAuth."
                },
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!uiState.hfSignedIn) {
                OutlinedTextField(
                    value = uiState.hfManualTokenInput,
                    onValueChange = viewModel::onHfManualTokenChange,
                    modifier = Modifier.padding(top = 8.dp),
                    label = { Text("HF access token (hf_…)") },
                    singleLine = true,
                )
                NibbliPrimaryButton(
                    text = "Save token",
                    onClick = { viewModel.saveManualHuggingFaceToken() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            if (uiState.hfConfigured && !uiState.hfSignedIn) {
                NibbliPrimaryButton(
                    text = "Sign in to Hugging Face",
                    onClick = {
                        viewModel.createHuggingFaceAuthIntent()?.let { hfLauncher.launch(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            if (uiState.hfSignedIn) {
                NibbliPrimaryButton(
                    text = "Sign out",
                    onClick = { viewModel.signOutHuggingFace() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            uiState.hfAuthMessage?.let { msg ->
                Text(
                    msg,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("Downloads", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Allow model downloads", modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.allowDownloads,
                    onCheckedChange = viewModel::setAllowDownloads,
                )
            }
        }
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("On-device models", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choose which installed LiteRT model powers chat, Assist, and Prompt Lab versus " +
                    "your Pixel Friend. Download more under Manage → Models.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            ModelPreferencePicker(
                label = "Default app model",
                hint = "Chat, Prompt Lab, and Assist when no per-screen override is set.",
                selectedModelId = uiState.defaultModelId,
                installedModelIds = uiState.installedModelIds,
                onSelect = viewModel::setDefaultModelId,
            )
            ModelPreferencePicker(
                label = "Pixel Friend model",
                hint = "Home talk, chips, and mood lines. Auto picks a small chat model if installed.",
                selectedModelId = uiState.petModelId,
                installedModelIds = uiState.installedModelIds,
                onSelect = viewModel::setPetModelId,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "LiteRT accelerator",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Auto uses per-model defaults (GPU when available). Emulators usually run on CPU.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                LiteRtAcceleratorPreference.entries.forEach { pref ->
                    NibbliSuggestionChip(
                        label = when (pref) {
                            LiteRtAcceleratorPreference.AUTO -> "auto (recommended)"
                            LiteRtAcceleratorPreference.GPU -> "gpu"
                            LiteRtAcceleratorPreference.CPU -> "cpu"
                            LiteRtAcceleratorPreference.NPU -> "npu"
                        },
                        selected = uiState.litertAccelerator == pref,
                        onClick = { viewModel.setLitertAccelerator(pref) },
                    )
                }
            }
        }
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("Pixel Friend", style = MaterialTheme.typography.titleMedium)
            Text(
                "Personality and ambient behavior. Talk uses the Pixel Friend model above.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Personality", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                PetPersonality.entries.forEach { p ->
                    NibbliSuggestionChip(
                        label = p.name.lowercase(),
                        selected = uiState.petPersonality == p,
                        onClick = { viewModel.setPetPersonality(p) },
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Comment on agent work",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = uiState.petCommentOnAgentWork,
                    onCheckedChange = viewModel::setPetCommentOnAgentWork,
                )
            }
            Text(
                "Mood pulse",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                PetMoodPulseMode.entries.forEach { mode ->
                    NibbliSuggestionChip(
                        label = when (mode) {
                            PetMoodPulseMode.OFF -> "off"
                            PetMoodPulseMode.NORMAL -> "normal"
                            PetMoodPulseMode.QUIET -> "quiet"
                        },
                        selected = uiState.petMoodPulseMode == mode,
                        onClick = { viewModel.setPetMoodPulseMode(mode) },
                    )
                }
            }
        }
        NibbliPrimaryButton(
            text = "Delete all chat history",
            onClick = { viewModel.clearChatHistory() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelPreferencePicker(
    label: String,
    hint: String,
    selectedModelId: String?,
    installedModelIds: List<String>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            hint,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (installedModelIds.isEmpty()) {
            Text(
                "No models installed yet.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            return@Column
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            NibbliSuggestionChip(
                label = "Auto",
                selected = selectedModelId == null,
                onClick = { onSelect(null) },
            )
            installedModelIds.forEach { modelId ->
                NibbliSuggestionChip(
                    label = ModelCatalog.displayName(modelId),
                    selected = selectedModelId == modelId,
                    onClick = { onSelect(modelId) },
                )
            }
        }
    }
}
