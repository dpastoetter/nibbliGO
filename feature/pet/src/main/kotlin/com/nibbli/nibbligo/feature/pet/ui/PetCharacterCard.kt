package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.pixel.LcdPickerEntry
import com.nibbli.nibbligo.feature.pet.ui.pixel.P1CoachMarksOverlay
import com.nibbli.nibbligo.feature.pet.ui.pixel.P1DeviceShell

@Composable
fun PetCharacterCard(
    pet: PetState,
    onPetTap: () -> Unit,
    onCareAction: (PetInteraction) -> Unit,
    onEquipLcdItem: (LcdPickerEntry) -> Unit,
    onLcdActivity: () -> Unit = {},
    onCareConfirm: () -> Unit = {},
    dialogueLine: String = "",
    isGeneratingDialogue: Boolean = false,
    talkLcdMode: Boolean = false,
    onDismissTalkLcd: () -> Unit = {},
    visitLabel: String? = null,
    visitPet: PetState? = null,
    carePet: PetState = pet,
    showCoachMarks: Boolean = false,
    onDismissCoachMarks: () -> Unit = {},
    openItemsModeRequest: Boolean = false,
    onItemsModeOpened: () -> Unit = {},
    highlightConfirm: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        P1DeviceShell(
            pet = pet,
            carePet = carePet,
            onPetTap = onPetTap,
            onCareAction = onCareAction,
            onCareConfirm = onCareConfirm,
            onEquipLcdItem = onEquipLcdItem,
            onLcdActivity = onLcdActivity,
            dialogueLine = dialogueLine,
            isGeneratingDialogue = isGeneratingDialogue,
            talkLcdMode = talkLcdMode,
            onDismissTalkLcd = onDismissTalkLcd,
            visitLabel = visitLabel,
            visitPet = visitPet,
            openItemsModeRequest = openItemsModeRequest,
            onItemsModeOpened = onItemsModeOpened,
            highlightConfirm = highlightConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        P1CoachMarksOverlay(
            visible = showCoachMarks,
            onDismiss = onDismissCoachMarks,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}
