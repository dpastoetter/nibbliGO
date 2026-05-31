package com.nibbli.nibbligo.feature.pet.ui.pixel

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState

data class P1MenuEntry(
    val label: String,
    val interaction: PetInteraction,
    val opensItemPicker: Boolean = false,
)

fun p1CareMenu(pet: PetState): List<P1MenuEntry> = buildList {
    add(P1MenuEntry("Meal", PetInteraction.FEED_MEAL))
    add(P1MenuEntry("Talk", PetInteraction.TALK))
    add(P1MenuEntry("Snack", PetInteraction.FEED_SNACK))
    add(P1MenuEntry("Play", PetInteraction.PLAY))
    add(P1MenuEntry("Clean", PetInteraction.CLEAN))
    add(P1MenuEntry("Meds", PetInteraction.MEDICINE))
    if (pet.condition == PetCondition.SLEEPING) {
        add(P1MenuEntry("Wake", PetInteraction.WAKE))
    } else {
        add(P1MenuEntry("Sleep", PetInteraction.SLEEP))
    }
    add(P1MenuEntry("Train", PetInteraction.TRAIN))
    add(P1MenuEntry("Items", PetInteraction.ITEMS, opensItemPicker = true))
}

fun P1MenuEntry.isConfirmEnabled(pet: PetState): Boolean {
    if (opensItemPicker) return pet.isAlive && pet.stage != LifeStage.EGG
    if (pet.condition == PetCondition.DEAD) return false
    if (pet.stage == LifeStage.EGG && interaction != PetInteraction.TALK) return false
    return pet.isAlive || interaction == PetInteraction.WAKE
}
