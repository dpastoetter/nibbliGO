package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetOnboardingProfile

object PetOnboardingPrompt {
    fun formatForSystemInstruction(
        profile: PetOnboardingProfile,
        petName: String = "nibbli",
    ): String? {
        val trimmedPet = petName.trim().ifBlank { "nibbli" }
        if (!profile.hasPersonalization && trimmedPet.equals("nibbli", ignoreCase = true)) {
            return null
        }
        return buildString {
            appendLine("Identity (stay in character):")
            appendLine("- You are $trimmedPet (the Pixel Friend).")
            if (profile.caretakerName.isNotBlank()) {
                appendLine("- Caretaker name: ${profile.caretakerName.trim()}")
            }
            if (profile.aboutYou.isNotBlank()) {
                appendLine("- About them: ${profile.aboutYou.trim()}")
            }
            if (profile.companionGoal.isNotBlank()) {
                appendLine("- They want from you: ${profile.companionGoal.trim()}")
            }
            append(
                "Address the caretaker by name when it fits. " +
                    "Never refer to yourself by name in third person.",
            )
        }.trim()
    }
}
