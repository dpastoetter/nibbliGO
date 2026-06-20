package com.nibbli.nibbligo.feature.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.core.designsystem.component.NibbliSecondaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.designsystem.component.NibbliTextField
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import com.nibbli.nibbligo.feature.settings.presentation.ParentalGateViewModel
import com.nibbli.nibbligo.feature.settings.presentation.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    gateViewModel: ParentalGateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gateActive by gateViewModel.gateActive.collectAsStateWithLifecycle()
    var hfUnlocked by remember { mutableStateOf(false) }
    var showHfGate by remember { mutableStateOf(false) }
    val hfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.onHuggingFaceAuthResult(result.data)
    }

    if (showHfGate) {
        ParentalGateDialog(
            onUnlocked = {
                showHfGate = false
                hfUnlocked = true
            },
            onDismiss = { showHfGate = false },
        )
    }
    NibbliScreen(modifier = modifier, scrollable = true) {
        NibbliScreenHeader(
            title = "Settings & Privacy",
            subtitle = "Appearance, storage, models, and privacy controls.",
            showOnDeviceBadge = true,
        )
        AppearanceCard(
            themeMode = uiState.themeMode,
            onThemeModeChange = viewModel::setThemeMode,
            accentPalette = uiState.accentPalette,
            onAccentPaletteChange = viewModel::setAccentPalette,
            fontScale = uiState.fontScale,
            onFontScaleChange = viewModel::setFontScale,
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
        if (gateActive && !hfUnlocked) {
            NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
                Text("Hugging Face", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Locked with the parent PIN. Sign-in and access tokens are managed by a parent.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NibbliSecondaryButton(
                    text = "Unlock with PIN",
                    onClick = { showHfGate = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        } else {
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
                NibbliTextField(
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
        }
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text("Pet notifications", style = MaterialTheme.typography.titleMedium)
            Text(
                "Alerts when your pet needs care, your streak is at risk, or the daily quest is unfinished.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Enable pet notifications",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = uiState.petNotificationsEnabled,
                    onCheckedChange = viewModel::setPetNotificationsEnabled,
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
                "Default model for Assist chat and other screens. Companion talk model is under Manage → Companion.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            ModelPreferencePicker(
                label = "Default app model",
                hint = "Chat when no per-screen override is set.",
                selectedModelId = uiState.defaultModelId,
                installedModelIds = uiState.installedModelIds,
                onSelect = viewModel::setDefaultModelId,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "LiteRT accelerator",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Auto tries GPU first (OpenCL), then CPU. On Pixel and other API 31+ phones, GPU needs " +
                    "vendor OpenCL libraries — nibbliGO declares these like Edge Gallery. Emulators stay on CPU.",
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
        NibbliPrimaryButton(
            text = "Delete all chat history",
            onClick = { viewModel.clearChatHistory() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
        )
    }
}
