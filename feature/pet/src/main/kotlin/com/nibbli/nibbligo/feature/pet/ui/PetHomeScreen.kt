package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.PetBubble
import com.nibbli.nibbligo.core.designsystem.component.StatBar
import com.nibbli.nibbligo.core.designsystem.theme.LavenderAccent
import com.nibbli.nibbligo.core.designsystem.theme.TealPrimary
import com.nibbli.nibbligo.core.designsystem.theme.WarmCoral
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.ui.LoadingState
import com.nibbli.nibbligo.feature.pet.presentation.PetViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: PetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.isLoading) {
        LoadingState(modifier)
        return
    }
    val pet = uiState.petState
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("nibbliGO", style = MaterialTheme.typography.displaySmall)
        Text("Your pocket AI companion", style = MaterialTheme.typography.bodyMedium)

        NibbliCard {
            PetCanvas(expression = pet.expression, modifier = Modifier
                .fillMaxWidth()
                .height(160.dp))
            PetBubble(text = pet.dialogueLine)
        }

        NibbliCard {
            Text("Stats", style = MaterialTheme.typography.titleMedium)
            StatBar("Hunger", pet.stats.hunger, Modifier.padding(top = 12.dp))
            StatBar("Energy", pet.stats.energy, Modifier.padding(top = 8.dp))
            StatBar("Mood", pet.stats.mood, Modifier.padding(top = 8.dp))
            StatBar("Trust", pet.stats.trust, Modifier.padding(top = 8.dp))
            StatBar("Curiosity", pet.stats.curiosity, Modifier.padding(top = 8.dp))
            StatBar("Skill", pet.stats.skill, Modifier.padding(top = 8.dp))
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PetInteraction.entries.forEach { interaction ->
                Button(onClick = { viewModel.onInteraction(interaction) }) {
                    Text(interaction.name.lowercase().replaceFirstChar { it.uppercase() })
                }
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
    }
}

@Composable
private fun PetCanvas(expression: PetExpression, modifier: Modifier = Modifier) {
    val bodyColor = when (expression) {
        PetExpression.HAPPY, PetExpression.PROUD -> TealPrimary
        PetExpression.SLEEPY, PetExpression.HUNGRY -> LavenderAccent
        PetExpression.CURIOUS -> WarmCoral
        else -> TealPrimary.copy(alpha = 0.85f)
    }
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        drawCircle(color = bodyColor, radius = size.minDimension * 0.35f, center = Offset(cx, cy))
        drawCircle(color = Color.White, radius = size.minDimension * 0.06f, center = Offset(cx - 40, cy - 20))
        drawCircle(color = Color.White, radius = size.minDimension * 0.06f, center = Offset(cx + 40, cy - 20))
        drawCircle(color = DeepSlateForCanvas, radius = size.minDimension * 0.025f, center = Offset(cx - 40, cy - 18))
        drawCircle(color = DeepSlateForCanvas, radius = size.minDimension * 0.025f, center = Offset(cx + 40, cy - 18))
    }
}

private val DeepSlateForCanvas = Color(0xFF1E2832)
