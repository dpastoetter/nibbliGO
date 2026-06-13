package com.nibbli.nibbligo.feature.pet.ui

import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetEngagementRules
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.collection.lcdItemDisplayName
import com.nibbli.nibbligo.feature.pet.ui.collection.lcdItemUnlockHint

object PetCelebrationDetector {

    fun detect(before: PetState, after: PetState): List<PetCelebrationEvent> {
        val events = mutableListOf<PetCelebrationEvent>()
        val questJustCompleted = !PetEngagementRules.dailyQuestComplete(before.engagement) &&
            PetEngagementRules.dailyQuestComplete(after.engagement) &&
            after.engagement.dailyQuestBonusClaimed &&
            !before.engagement.dailyQuestBonusClaimed
        if (questJustCompleted) {
            events.add(PetCelebrationEvent.DailyQuestComplete)
        }
        (after.unlockedCosmetics - before.unlockedCosmetics).forEach { cosmetic ->
            events.add(cosmeticUnlockEvent(cosmetic))
        }
        (after.unlockedScenes - before.unlockedScenes).forEach { scene ->
            events.add(sceneUnlockEvent(scene))
        }
        (after.unlockedProps - before.unlockedProps).forEach { prop ->
            events.add(propUnlockEvent(prop))
        }
        return events
    }

    private fun cosmeticUnlockEvent(cosmetic: PetCosmetic): PetCelebrationEvent =
        PetCelebrationEvent.ItemUnlocked(
            displayName = lcdItemDisplayName(cosmetic),
            kind = PetCelebrationEvent.UnlockKind.COSMETIC,
            hint = lcdItemUnlockHint(cosmetic),
        )

    private fun sceneUnlockEvent(scene: PetLcdScene): PetCelebrationEvent =
        PetCelebrationEvent.ItemUnlocked(
            displayName = lcdItemDisplayName(scene),
            kind = PetCelebrationEvent.UnlockKind.SCENE,
            hint = lcdItemUnlockHint(scene),
        )

    private fun propUnlockEvent(prop: PetLcdProp): PetCelebrationEvent =
        PetCelebrationEvent.ItemUnlocked(
            displayName = lcdItemDisplayName(prop),
            kind = PetCelebrationEvent.UnlockKind.PROP,
            hint = lcdItemUnlockHint(prop),
        )
}
