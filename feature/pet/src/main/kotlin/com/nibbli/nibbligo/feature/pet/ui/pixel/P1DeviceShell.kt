package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.domain.forVisitPlaydate
import kotlinx.coroutines.delay

private enum class P1ShellMode {
    CARE,
    ITEMS,
}

@Composable
fun P1DeviceShell(
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
    val menu = p1CareMenu(carePet)
    var menuIndex by remember(carePet.condition, menu.size) { mutableIntStateOf(0) }
    val safeIndex = menuIndex.coerceIn(0, (menu.size - 1).coerceAtLeast(0))
    val current = menu[safeIndex]

    var shellMode by remember { mutableStateOf(P1ShellMode.CARE) }
    var pickerIndex by remember(carePet.unlockedCosmetics, carePet.unlockedScenes, carePet.unlockedProps) {
        mutableIntStateOf(0)
    }
    val pickerEntries = remember(carePet) { carePet.lcdItemPickerEntries() }
    val safePickerIndex = pickerIndex.coerceIn(0, (pickerEntries.size - 1).coerceAtLeast(0))
    val currentPicker = pickerEntries.getOrNull(safePickerIndex)

    var flash by remember { mutableStateOf(false) }
    var motionBoost by remember { mutableStateOf(false) }
    var frameIndex by remember { mutableIntStateOf(0) }
    val inItemMode = shellMode == P1ShellMode.ITEMS
    val showDualVisit = visitPet != null && !talkLcdMode && !inItemMode
    val lcdPet = when {
        inItemMode && currentPicker != null -> carePet.withPickerPreview(currentPicker)
        showDualVisit -> carePet.forVisitPlaydate()
        else -> pet
    }
    val lcdVisitPet = if (showDualVisit) visitPet else null
    val selection = lcdPet.resolveSprite()
    val spriteSequence = selection.toSequence()

    LaunchedEffect(lcdPet.isAlive, selection.primary, selection.alternate, lcdPet.animation) {
        frameIndex = 0
        if (!lcdPet.isAlive) {
            return@LaunchedEffect
        }
        while (true) {
            delay(spriteSequence.stepMs)
            frameIndex += 1
        }
    }

    LaunchedEffect(flash) {
        if (flash) {
            delay(100)
            flash = false
        }
    }

    LaunchedEffect(motionBoost) {
        if (motionBoost) {
            delay(1_100)
            motionBoost = false
        }
    }

    fun triggerMotionBoost() {
        motionBoost = true
    }

    val canCycle = carePet.isAlive && !inItemMode
    val canConfirm = if (inItemMode) {
        currentPicker != null && !currentPicker.isLocked
    } else {
        current.isConfirmEnabled(carePet)
    }
    val lcdMenuLabel = when {
        inItemMode -> currentPicker?.menuLabel ?: "ITEMS"
        else -> current.label
    }
    val colors = p1Colors()
    val showTalkOverlay = talkLcdMode && (dialogueLine.isNotBlank() || isGeneratingDialogue)

    NibbliCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(P1Theme.LcdWellRadius))
                    .background(colors.lcdWell)
                    .padding(P1Theme.LcdMargin)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = carePet.isAlive,
                        onClick = {
                            triggerMotionBoost()
                            onPetTap()
                        },
                    ),
            ) {
                P1LcdCanvas(
                    pet = lcdPet,
                    visitPet = lcdVisitPet,
                    menuLabel = lcdMenuLabel,
                    frameIndex = frameIndex,
                    flash = flash,
                    tapBoost = motionBoost,
                    talkLcdMode = talkLcdMode,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showTalkOverlay) {
                    P1LcdDialogueOverlay(
                        text = dialogueLine,
                        isLoading = isGeneratingDialogue && dialogueLine.isBlank(),
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

            P1ThreeButtonBar(
                onButtonLeft = {
                    onDismissTalkLcd()
                    onLcdActivity()
                    if (inItemMode) {
                        shellMode = P1ShellMode.CARE
                    } else {
                        menuIndex = (safeIndex - 1 + menu.size) % menu.size
                    }
                },
                onButtonCenter = {
                    onDismissTalkLcd()
                    if (!canConfirm) return@P1ThreeButtonBar
                    flash = true
                    triggerMotionBoost()
                    if (inItemMode) {
                        currentPicker?.let(onEquipLcdItem)
                    } else if (current.opensItemPicker) {
                        shellMode = P1ShellMode.ITEMS
                        pickerIndex = pickerEntries.indexOfFirst { it.isCurrentlyEquipped(carePet) }
                            .coerceAtLeast(0)
                    } else {
                        onCareAction(current.interaction)
                    }
                },
                onButtonRight = {
                    onDismissTalkLcd()
                    onLcdActivity()
                    if (inItemMode) {
                        if (pickerEntries.isNotEmpty()) {
                            pickerIndex = (safePickerIndex + 1) % pickerEntries.size
                        }
                    } else {
                        menuIndex = (safeIndex + 1) % menu.size
                    }
                },
                cycleEnabled = carePet.isAlive,
                confirmEnabled = canConfirm,
                modifier = Modifier.padding(top = 12.dp),
            )
            visitLabel?.let { name ->
                Text(
                    text = "Visiting $name",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    ),
                    color = colors.footerText,
                )
            }
        }
    }
}
