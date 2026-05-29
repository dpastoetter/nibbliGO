package com.nibbli.nibbligo.feature.pet.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.PetBubble
import com.nibbli.nibbligo.core.designsystem.component.StatBar
import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.ui.LoadingState
import com.nibbli.nibbligo.feature.pet.presentation.PetViewModel
import com.nibbli.nibbligo.feature.pet.ui.pixel.PixelDeviceFrame

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: PetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(uiState.agentToast) {
        uiState.agentToast?.let {
            snackbar.showSnackbar(it)
            viewModel.clearAgentToast()
        }
    }

    if (uiState.isLoading) {
        LoadingState(modifier)
        return
    }

    val pet = uiState.petState

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("nibbliGO", style = MaterialTheme.typography.displaySmall)
            Text(
                "${pet.name} · ${pet.stage.name.lowercase()} · on-device",
                style = MaterialTheme.typography.bodyMedium,
            )
            uiState.statusMessage?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            NibbliCard {
                PixelDeviceFrame(
                    roomId = pet.roomId,
                    stage = pet.stage,
                    expression = pet.expression,
                    animation = pet.animation,
                    condition = pet.condition,
                    activeNeed = pet.activeNeed,
                    modifier = Modifier.fillMaxWidth(),
                )
                PetBubble(
                    text = if (uiState.isGeneratingDialogue) "…" else pet.dialogueLine,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NibbliCard {
                Text("Stats", style = MaterialTheme.typography.titleMedium)
                StatBar("Hunger", pet.stats.hunger, Modifier.padding(top = 8.dp))
                StatBar("Happy", pet.stats.happiness, Modifier.padding(top = 6.dp))
                StatBar("Energy", pet.stats.energy, Modifier.padding(top = 6.dp))
                StatBar("Hygiene", pet.stats.hygiene, Modifier.padding(top = 6.dp))
                StatBar("Health", pet.stats.health, Modifier.padding(top = 6.dp))
                StatBar("Trust", pet.stats.trust, Modifier.padding(top = 6.dp))
                StatBar("Skill", pet.stats.skill, Modifier.padding(top = 6.dp))
                Text(
                    "Care ${pet.careScore} · Age ${pet.ageMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            NibbliCard {
                Text("Care", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    careButton("Meal", PetInteraction.FEED_MEAL, pet, viewModel)
                    careButton("Snack", PetInteraction.FEED_SNACK, pet, viewModel)
                    careButton("Play", PetInteraction.PLAY, pet, viewModel)
                    careButton("Clean", PetInteraction.CLEAN, pet, viewModel)
                    careButton("Meds", PetInteraction.MEDICINE, pet, viewModel)
                    if (pet.condition == PetCondition.SLEEPING) {
                        careButton("Wake", PetInteraction.WAKE, pet, viewModel)
                    } else {
                        careButton("Sleep", PetInteraction.SLEEP, pet, viewModel)
                    }
                    careButton("Talk", PetInteraction.TALK, pet, viewModel)
                    careButton("Train", PetInteraction.TRAIN, pet, viewModel)
                }
                OutlinedButton(
                    onClick = { viewModel.openMinigame() },
                    modifier = Modifier.padding(top = 8.dp),
                    enabled = pet.isAlive,
                ) { Text("Catch game") }
            }

            if (pet.condition == PetCondition.DEAD) {
                NibbliCard {
                    Text("nibbli needs a fresh start", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { viewModel.hatchNewEgg() },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("Hatch new egg") }
                }
            }

            if (pet.unlockedCosmetics.isNotEmpty()) {
                NibbliCard {
                    Text("Unlocked looks", style = MaterialTheme.typography.titleMedium)
                    pet.unlockedCosmetics.forEach { cosmetic ->
                        Text("• ${cosmetic.name.replace('_', ' ')}", modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent.createChooser(viewModel.exportDiary(), "Export diary"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export pet diary") }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.padding(8.dp))
    }

    PetTalkSheet(
        visible = uiState.showTalkSheet,
        isGenerating = uiState.isGeneratingDialogue,
        onDismiss = { viewModel.dismissTalkSheet() },
        onSend = { viewModel.onTalkSend(it) },
    )
    PetMinigameDialog(
        visible = uiState.showMinigame,
        onDismiss = { viewModel.dismissMinigame() },
        onWin = { viewModel.onMinigameWin() },
    )
}

@Composable
private fun careButton(
    label: String,
    interaction: PetInteraction,
    pet: com.nibbli.nibbligo.core.model.PetState,
    viewModel: PetViewModel,
) {
    val enabled = pet.isAlive || interaction == PetInteraction.WAKE
    Button(
        onClick = { viewModel.onInteraction(interaction) },
        enabled = enabled && !(pet.stage == LifeStage.EGG && interaction != PetInteraction.TALK),
    ) { Text(label) }
}
