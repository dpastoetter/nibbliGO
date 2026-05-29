package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.StatBar
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState
import kotlinx.coroutines.delay

private val NibbliTeal = Color(0xFF3D9A8B)
private val NibbliCoral = Color(0xFFE07A5F)
private val NibbliGreen = Color(0xFF4CAF50)

@Composable
fun PetCharacterCard(
    pet: PetState,
    onPetTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tapPulse by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (tapPulse) 1.08f else 1f,
        animationSpec = tween(150),
        label = "pet_tap_scale",
    )

    LaunchedEffect(tapPulse) {
        if (tapPulse) {
            delay(150)
            tapPulse = false
        }
    }

    val roomTint = when (pet.roomId) {
        "sunset" -> Color(0xFFE8B4A0).copy(alpha = 0.25f)
        "forest" -> Color(0xFFA8C69A).copy(alpha = 0.25f)
        else -> Color(0xFF9EAD86).copy(alpha = 0.2f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(roomTint),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = pet.portraitRes),
                contentDescription = pet.name,
                modifier = Modifier
                    .size(240.dp)
                    .scale(scale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = pet.isAlive,
                    ) {
                        tapPulse = true
                        onPetTap()
                    },
            )
            if (pet.activeNeed != PetNeed.NONE && pet.condition != PetCondition.DEAD) {
                Text(
                    text = needIcon(pet.activeNeed),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                )
            }
        }

        StatBar(
            label = "Happy",
            value = pet.stats.happiness,
            color = NibbliTeal,
            modifier = Modifier.padding(top = 16.dp),
        )
        StatBar(
            label = "Hunger",
            value = pet.stats.hunger,
            color = NibbliCoral,
            modifier = Modifier.padding(top = 8.dp),
        )
        StatBar(
            label = "Energy",
            value = pet.stats.energy,
            color = NibbliGreen,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "CARE ${pet.careScore} · AGE ${pet.ageMinutes}m",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
