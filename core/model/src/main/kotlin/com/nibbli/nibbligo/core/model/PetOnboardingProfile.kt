package com.nibbli.nibbligo.core.model

/** Caretaker answers collected during first-run onboarding. */
data class PetOnboardingProfile(
    val caretakerName: String = "",
    val aboutYou: String = "",
    val companionGoal: String = "",
    val completed: Boolean = false,
) {
    val hasPersonalization: Boolean
        get() = caretakerName.isNotBlank() || aboutYou.isNotBlank() || companionGoal.isNotBlank()
}
