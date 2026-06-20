package com.nibbli.nibbligo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nibbli.nibbligo.core.designsystem.component.NibbliActionTile
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliComingSoonCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.feature.settings.presentation.ParentalGateViewModel
import com.nibbli.nibbligo.feature.settings.ui.ParentalGateDialog
import com.nibbli.nibbligo.navigation.Routes

@Composable
fun SenseHubScreen(navController: NavController) {
    NibbliScreen {
        NibbliScreenHeader(
            title = "Sense",
            subtitle = "Multimodal vision and audio on-device.",
        )
        NibbliCard {
            Text(
                "Vision and audio scribe use installed LiteRT models when available.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        NibbliComingSoonCard(
            title = "Sense hub",
            body = "Ask Image (camera/gallery picker) and Audio Scribe are in development. " +
                "They will run on-device when multimodal models are ready.",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NibbliActionTile(
                icon = Icons.Outlined.Image,
                label = "Ask Image",
                onClick = { navController.navigate(Routes.ASK_IMAGE) },
                modifier = Modifier.weight(1f),
            )
            NibbliActionTile(
                icon = Icons.Outlined.Mic,
                label = "Audio Scribe",
                onClick = { navController.navigate(Routes.AUDIO_SCRIBE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun ManageHubScreen(
    navController: NavController,
    gateViewModel: ParentalGateViewModel = hiltViewModel(),
) {
    val gateActive by gateViewModel.gateActive.collectAsStateWithLifecycle()
    var pendingGatedRoute by remember { mutableStateOf<String?>(null) }

    fun navigateGated(route: String) {
        if (gateActive) pendingGatedRoute = route else navController.navigate(route)
    }

    pendingGatedRoute?.let { route ->
        ParentalGateDialog(
            onUnlocked = {
                pendingGatedRoute = null
                navController.navigate(route)
            },
            onDismiss = { pendingGatedRoute = null },
        )
    }

    NibbliScreen {
        NibbliScreenHeader(
            title = "Manage",
            subtitle = "Models, companion, learning tools, and privacy.",
        )
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                "Download LiteRT models, tune your Pixel Friend, benchmark on your phone, " +
                    "and tweak prompts — all on-device.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NibbliActionTile(
                icon = Icons.Outlined.Storage,
                label = "Models",
                onClick = { navController.navigate(Routes.MODELS) },
                modifier = Modifier.weight(1f),
            )
            NibbliActionTile(
                icon = Icons.Outlined.SmartToy,
                label = "Companion",
                onClick = { navController.navigate(Routes.COMPANION) },
                modifier = Modifier.weight(1f),
            )
            NibbliActionTile(
                icon = Icons.Outlined.Collections,
                label = "Collection",
                onClick = { navController.navigate(Routes.PET_COLLECTION) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NibbliActionTile(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                onClick = { navController.navigate(Routes.SETTINGS) },
                modifier = Modifier.weight(1f),
            )
            NibbliActionTile(
                icon = Icons.Outlined.FamilyRestroom,
                label = "For parents",
                onClick = { navController.navigate(Routes.PARENT_CONTROLS) },
                modifier = Modifier.weight(1f),
            )
        }
        NibbliComingSoonCard(
            title = "Do hub & MCP actions",
            body = "Phone tool actions and MCP servers are available from Agent Chat today. " +
                "A dedicated Do tab returns when the hub is product-ready.",
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Learn edge AI",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (gateActive) {
                LockedHint()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NibbliActionTile(
                icon = Icons.Outlined.Speed,
                label = "Benchmark",
                onClick = { navigateGated(Routes.BENCHMARK) },
                modifier = Modifier.weight(1f),
            )
            NibbliActionTile(
                icon = Icons.Outlined.Science,
                label = "Prompt Lab",
                onClick = { navigateGated(Routes.PROMPT_LAB) },
                modifier = Modifier.weight(1f),
            )
            NibbliActionTile(
                icon = Icons.Outlined.AutoAwesome,
                label = "Agent",
                onClick = { navigateGated(Routes.AGENT) },
                modifier = Modifier.weight(1f),
            )
        }
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                "Benchmark compares TTFT and tokens/sec on your phone. Prompt Lab shows " +
                    "exact prompts for A/B tests. Agent handles email and calendar with confirm-before-run." +
                    if (gateActive) " These tools are locked with the parent PIN." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "Help & learning",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, start = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NibbliActionTile(
                icon = Icons.Outlined.HelpOutline,
                label = "FAQ",
                onClick = { navController.navigate(Routes.FAQ_NIBBLIGO) },
                modifier = Modifier.weight(1f),
            )
            NibbliActionTile(
                icon = Icons.Outlined.MenuBook,
                label = "AI basics",
                onClick = { navController.navigate(Routes.FAQ_AI_BASICS) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LockedHint() {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = "Locked with parent PIN",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "PIN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
