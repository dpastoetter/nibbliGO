package com.nibbli.nibbligo.feature.pet.ui.pixel

import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.equippedScene
import com.nibbli.nibbligo.core.model.withEquippedScene

sealed class LcdPickerEntry {
    abstract val menuLabel: String
    abstract val isLocked: Boolean

    data object WearNone : LcdPickerEntry() {
        override val menuLabel = "NONE"
        override val isLocked = false
    }

    data class Wearable(val cosmetic: PetCosmetic) : LcdPickerEntry() {
        override val menuLabel = cosmetic.menuLabel
        override val isLocked = false
    }

    data object SceneNone : LcdPickerEntry() {
        override val menuLabel = "PLAIN"
        override val isLocked = false
    }

    data class Scene(val scene: PetLcdScene) : LcdPickerEntry() {
        override val menuLabel = scene.menuLabel
        override val isLocked = false
    }

    data object PropNone : LcdPickerEntry() {
        override val menuLabel = "FLOOR"
        override val isLocked = false
    }

    data class Prop(val prop: PetLcdProp) : LcdPickerEntry() {
        override val menuLabel = prop.menuLabel
        override val isLocked = false
    }

    data class LockedPreview(override val menuLabel: String) : LcdPickerEntry() {
        override val isLocked = true
    }
}

private val PetCosmetic.menuLabel: String
    get() = when (this) {
        PetCosmetic.SPARKLE_COLLAR -> "COLLAR"
        PetCosmetic.STAR_PATCH -> "PATCH"
        PetCosmetic.AURORA_AURA -> "AURORA"
    }

fun PetState.lcdItemPickerEntries(): List<LcdPickerEntry> = buildList {
    add(LcdPickerEntry.WearNone)
    PetCosmetic.entries.forEach { cosmetic ->
        if (cosmetic in unlockedCosmetics) {
            add(LcdPickerEntry.Wearable(cosmetic))
        } else {
            add(LcdPickerEntry.LockedPreview("LOCKED"))
        }
    }
    add(LcdPickerEntry.SceneNone)
    PetLcdScene.entries.forEach { scene ->
        if (scene in unlockedScenes) {
            add(LcdPickerEntry.Scene(scene))
        } else {
            add(LcdPickerEntry.LockedPreview("LOCKED"))
        }
    }
    add(LcdPickerEntry.PropNone)
    PetLcdProp.entries.forEach { prop ->
        if (prop in unlockedProps) {
            add(LcdPickerEntry.Prop(prop))
        } else {
            add(LcdPickerEntry.LockedPreview("LOCKED"))
        }
    }
}

fun PetState.withPickerPreview(entry: LcdPickerEntry): PetState = when (entry) {
    LcdPickerEntry.WearNone -> copy(equippedCosmetic = null)
    is LcdPickerEntry.Wearable -> copy(equippedCosmetic = entry.cosmetic)
    LcdPickerEntry.SceneNone -> withEquippedScene(PetLcdScene.COZY)
    is LcdPickerEntry.Scene -> withEquippedScene(entry.scene)
    LcdPickerEntry.PropNone -> copy(equippedProp = null)
    is LcdPickerEntry.Prop -> copy(equippedProp = entry.prop)
    is LcdPickerEntry.LockedPreview -> this
}

fun PetState.applyPickerEquip(entry: LcdPickerEntry): PetState? {
    if (entry.isLocked) return null
    return when (entry) {
        LcdPickerEntry.WearNone -> copy(equippedCosmetic = null)
        is LcdPickerEntry.Wearable -> {
            if (entry.cosmetic !in unlockedCosmetics) return null
            copy(equippedCosmetic = entry.cosmetic)
        }
        LcdPickerEntry.SceneNone -> withEquippedScene(PetLcdScene.COZY)
        is LcdPickerEntry.Scene -> {
            if (entry.scene !in unlockedScenes) return null
            withEquippedScene(entry.scene)
        }
        LcdPickerEntry.PropNone -> copy(equippedProp = null)
        is LcdPickerEntry.Prop -> {
            if (entry.prop !in unlockedProps) return null
            copy(equippedProp = entry.prop)
        }
        is LcdPickerEntry.LockedPreview -> null
    }
}

fun LcdPickerEntry.isCurrentlyEquipped(state: PetState): Boolean = when (this) {
    LcdPickerEntry.WearNone -> state.equippedCosmetic == null
    is LcdPickerEntry.Wearable -> state.equippedCosmetic == cosmetic
    LcdPickerEntry.SceneNone -> state.equippedScene == PetLcdScene.COZY
    is LcdPickerEntry.Scene -> state.equippedScene == scene
    LcdPickerEntry.PropNone -> state.equippedProp == null
    is LcdPickerEntry.Prop -> state.equippedProp == prop
    is LcdPickerEntry.LockedPreview -> false
}
