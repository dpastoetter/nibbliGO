package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import kotlinx.coroutines.delay

@Composable
fun P1DeviceShell(
    pet: PetState,
    onPetTap: () -> Unit,
    onCareAction: (PetInteraction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val menu = p1CareMenu(pet)
    var menuIndex by remember(pet.condition, menu.size) { mutableIntStateOf(0) }
    val safeIndex = menuIndex.coerceIn(0, (menu.size - 1).coerceAtLeast(0))
    val current = menu[safeIndex]

    var flash by remember { mutableStateOf(false) }
    var motionBoost by remember { mutableStateOf(false) }
    var frameIndex by remember { mutableIntStateOf(0) }
    val selection = pet.resolveSprite()
    val spriteSequence = selection.toSequence()

    LaunchedEffect(pet.isAlive, selection.primary, selection.alternate, pet.animation) {
        if (!pet.isAlive) {
            frameIndex = 0
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
            delay(600)
            motionBoost = false
        }
    }

    fun triggerMotionBoost() {
        motionBoost = true
    }

    val canCycle = pet.isAlive
    val canConfirm = current.isConfirmEnabled(pet)
    val colors = p1Colors()

    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(P1Theme.ShellRadius))
            .clip(RoundedCornerShape(P1Theme.ShellRadius))
            .background(colors.shellBody)
            .border(1.dp, colors.shellBorder, RoundedCornerShape(P1Theme.ShellRadius))
            .padding(P1Theme.ShellPadding),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.lcdWell)
                .padding(P1Theme.LcdMargin)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = pet.isAlive,
                    onClick = {
                        triggerMotionBoost()
                        onPetTap()
                    },
                ),
        ) {
            P1LcdCanvas(
                pet = pet,
                menuLabel = current.label,
                frameIndex = frameIndex,
                flash = flash,
                tapBoost = motionBoost,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        P1ThreeButtonBar(
            onButtonLeft = { menuIndex = (safeIndex - 1 + menu.size) % menu.size },
            onButtonCenter = {
                if (canConfirm) {
                    flash = true
                    triggerMotionBoost()
                    onCareAction(current.interaction)
                }
            },
            onButtonRight = { menuIndex = (safeIndex + 1) % menu.size },
            cycleEnabled = canCycle,
            confirmEnabled = canConfirm,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
