package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.OnDeviceBadge
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Settings & Privacy", style = MaterialTheme.typography.displaySmall)
        OnDeviceBadge(Modifier.padding(vertical = 12.dp))
        NibbliCard {
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
                Button(
                    onClick = { viewModel.saveManualHuggingFaceToken() },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Save token") }
            }
            if (uiState.hfConfigured && !uiState.hfSignedIn) {
                Button(
                    onClick = {
                        viewModel.createHuggingFaceAuthIntent()?.let { hfLauncher.launch(it) }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Sign in to Hugging Face") }
            }
            if (uiState.hfSignedIn) {
                Button(
                    onClick = { viewModel.signOutHuggingFace() },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("Sign out") }
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
            androidx.compose.foundation.layout.Row(
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
            Text("Pixel Friend", style = MaterialTheme.typography.titleMedium)
            Text(
                "Talk and care reactions use your installed LiteRT model.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Personality", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                PetPersonality.entries.forEach { p ->
                    val selected = uiState.petPersonality == p
                    if (selected) {
                        Button(onClick = { viewModel.setPetPersonality(p) }) {
                            Text(p.name.lowercase())
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.setPetPersonality(p) }) {
                            Text(p.name.lowercase())
                        }
                    }
                }
            }
        }
        Button(
            onClick = { viewModel.clearChatHistory() },
            modifier = Modifier.padding(top = 16.dp),
        ) { Text("Delete all chat history") }
    }
}
