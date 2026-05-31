package com.nibbli.nibbligo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nibbli.nibbligo.core.designsystem.component.NibbliActionTile
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
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
fun ManageHubScreen(navController: NavController) {
    NibbliScreen {
        NibbliScreenHeader(
            title = "Manage",
            subtitle = "Models, companion, and privacy controls.",
        )
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                "Download LiteRT models, tune your Pixel Friend, and adjust app settings.",
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
                icon = Icons.Outlined.Settings,
                label = "Settings",
                onClick = { navController.navigate(Routes.SETTINGS) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
