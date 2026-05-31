package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.pixel.LcdPickerEntry
import com.nibbli.nibbligo.feature.pet.ui.pixel.P1DeviceShell

@Composable
fun PetCharacterCard(
    pet: PetState,
    onPetTap: () -> Unit,
    onCareAction: (PetInteraction) -> Unit,
    onEquipLcdItem: (LcdPickerEntry) -> Unit,
    onLcdActivity: () -> Unit = {},
    dialogueLine: String = "",
    isGeneratingDialogue: Boolean = false,
    talkLcdMode: Boolean = false,
    onDismissTalkLcd: () -> Unit = {},
    visitLabel: String? = null,
    visitPet: PetState? = null,
    carePet: PetState = pet,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = false,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                P1DeviceShell(
                    pet = pet,
                    carePet = carePet,
                    onPetTap = onPetTap,
                    onCareAction = onCareAction,
                    onEquipLcdItem = onEquipLcdItem,
                    onLcdActivity = onLcdActivity,
                    dialogueLine = dialogueLine,
                    isGeneratingDialogue = isGeneratingDialogue,
                    talkLcdMode = talkLcdMode,
                    onDismissTalkLcd = onDismissTalkLcd,
                    visitLabel = visitLabel,
                    visitPet = visitPet,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
