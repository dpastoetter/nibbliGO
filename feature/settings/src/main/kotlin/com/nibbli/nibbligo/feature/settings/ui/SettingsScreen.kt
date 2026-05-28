package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.nibbli.nibbligo.core.designsystem.component.OnDeviceBadge
import com.nibbli.nibbligo.feature.settings.presentation.SettingsViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
            Text("Runtime", style = MaterialTheme.typography.titleMedium)
            Text(
                "Use LiteRT-LM when a .litertlm model is in files/models/. " +
                    "Otherwise nibbliGO uses the demo fake runtime.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Prefer LiteRT runtime", modifier = Modifier.weight(1f))
                Switch(
                    checked = uiState.useLiteRtRuntime,
                    onCheckedChange = viewModel::setUseLiteRt,
                )
            }
        }
        Button(
            onClick = { viewModel.clearChatHistory() },
            modifier = Modifier.padding(top = 16.dp),
        ) { Text("Delete all chat history") }
    }
}
