package com.nibbli.nibbligo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nibbli.nibbligo.navigation.Routes

@Composable
fun AssistHubScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Assist", style = MaterialTheme.typography.displaySmall)
        Text("Chat and experiment with prompts — all on-device.")
        Button(onClick = { navController.navigate(Routes.CHAT) }) { Text("Local Chat") }
        Button(onClick = { navController.navigate(Routes.AGENT_CHAT) }) { Text("Agent Chat") }
        Button(onClick = { navController.navigate(Routes.PROMPT_LAB) }) { Text("Prompt Lab") }
    }
}

@Composable
fun SenseHubScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sense", style = MaterialTheme.typography.displaySmall)
        Text("See and hear the world through local models.")
        Button(onClick = { navController.navigate(Routes.ASK_IMAGE) }) { Text("Ask Image") }
        Button(onClick = { navController.navigate(Routes.AUDIO_SCRIBE) }) { Text("Audio Scribe") }
    }
}

@Composable
fun ManageHubScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Manage", style = MaterialTheme.typography.displaySmall)
        Text("Models, benchmarks, and privacy controls.")
        Button(onClick = { navController.navigate(Routes.MODELS) }) { Text("Models") }
        Button(onClick = { navController.navigate(Routes.BENCHMARK) }) { Text("Benchmark") }
        Button(onClick = { navController.navigate(Routes.SETTINGS) }) { Text("Settings") }
    }
}
