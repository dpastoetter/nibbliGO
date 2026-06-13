package com.nibbli.nibbligo.feature.pet.ui.collection

import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetState

fun lcdItemDisplayName(cosmetic: PetCosmetic): String = when (cosmetic) {
    PetCosmetic.SPARKLE_COLLAR -> "Sparkle collar"
    PetCosmetic.STAR_PATCH -> "Star patch"
    PetCosmetic.AURORA_AURA -> "Aurora aura"
}

fun lcdItemDisplayName(scene: PetLcdScene): String = when (scene) {
    PetLcdScene.COZY -> "Cozy scene"
    PetLcdScene.STARS -> "Stars scene"
    PetLcdScene.CLOUDS -> "Clouds scene"
    PetLcdScene.NIGHT -> "Night scene"
}

fun lcdItemDisplayName(prop: PetLcdProp): String = when (prop) {
    PetLcdProp.BALL -> "Ball"
    PetLcdProp.PLANT -> "Plant"
    PetLcdProp.BLANKET -> "Mat"
}

fun lcdItemUnlockHint(cosmetic: PetCosmetic): String =
    "Reach skill ${cosmetic.unlockSkill} and trust ${cosmetic.unlockTrust}"

fun lcdItemUnlockHint(scene: PetLcdScene): String = when (scene) {
    PetLcdScene.COZY -> "Default scene"
    PetLcdScene.STARS -> "Care score ${scene.unlockCareScore}+"
    PetLcdScene.CLOUDS -> "Care ${scene.unlockCareScore}+ and ${scene.unlockStage?.name?.lowercase()} stage"
    PetLcdScene.NIGHT -> "Care ${scene.unlockCareScore}+ and adult stage"
}

fun lcdItemUnlockHint(prop: PetLcdProp): String = when (prop) {
    PetLcdProp.BALL -> "Win an arcade minigame"
    PetLcdProp.PLANT -> "Complete daily quest or care milestones"
    PetLcdProp.BLANKET -> "Complete daily quest or care milestones"
}

data class LcdCollectionEntry(
    val id: String,
    val displayName: String,
    val category: String,
    val unlocked: Boolean,
    val unlockHint: String,
)

fun PetState.lcdCollectionEntries(): List<LcdCollectionEntry> = buildList {
    PetCosmetic.entries.forEach { cosmetic ->
        add(
            LcdCollectionEntry(
                id = "cosmetic_${cosmetic.name}",
                displayName = lcdItemDisplayName(cosmetic),
                category = "Wearable",
                unlocked = cosmetic in unlockedCosmetics,
                unlockHint = lcdItemUnlockHint(cosmetic),
            ),
        )
    }
    PetLcdScene.entries.forEach { scene ->
        add(
            LcdCollectionEntry(
                id = "scene_${scene.name}",
                displayName = lcdItemDisplayName(scene),
                category = "Scene",
                unlocked = scene in unlockedScenes,
                unlockHint = lcdItemUnlockHint(scene),
            ),
        )
    }
    PetLcdProp.entries.forEach { prop ->
        add(
            LcdCollectionEntry(
                id = "prop_${prop.name}",
                displayName = lcdItemDisplayName(prop),
                category = "Floor prop",
                unlocked = prop in unlockedProps,
                unlockHint = lcdItemUnlockHint(prop),
            ),
        )
    }
}
