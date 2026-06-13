package com.nibbli.nibbligo.feature.pet.ui

sealed class PetCelebrationEvent {
    data object DailyQuestComplete : PetCelebrationEvent()

    data class ItemUnlocked(
        val displayName: String,
        val kind: UnlockKind,
        val hint: String? = null,
    ) : PetCelebrationEvent()

    enum class UnlockKind {
        COSMETIC,
        SCENE,
        PROP,
    }
}
