package com.nibbli.nibbligo.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : TopLevelDestination("pet", "Home", Icons.Default.Home)
    data object Assist : TopLevelDestination("chat", "Chat", Icons.Default.AutoAwesome)
    data object Sense : TopLevelDestination("sense", "Sense", Icons.Default.Mic)
    data object Do : TopLevelDestination("do", "Do", Icons.Outlined.Bolt)
    data object Manage : TopLevelDestination("manage", "Manage", Icons.Default.Settings)

    companion object {
        val entries = listOf(Home, Assist, Sense, Do, Manage)
    }
}

object Routes {
    const val PET = "pet"
    const val CHAT = "chat"
    const val SENSE = "sense"
    const val ASK_IMAGE = "ask_image"
    const val AUDIO_SCRIBE = "audio_scribe"
    const val DO = "do"
    const val MANAGE = "manage"
    const val ACTIONS = "actions"
    const val MODELS = "models"
    const val BENCHMARK = "benchmark"
    const val SETTINGS = "settings"
    const val COMPANION = "companion"
    const val AGENT = "agent"
    const val PROMPT_LAB = "prompt_lab"
    const val FAQ_NIBBLIGO = "faq_nibbliGO"
    const val FAQ_AI_BASICS = "faq_ai_basics"
}
