package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetOnboardingProfile

object PetOnboardingPrompt {
    fun formatForSystemInstruction(profile: PetOnboardingProfile): String? {
        if (!profile.hasPersonalization) return null
        return buildString {
            appendLine("Caretaker profile (use naturally, stay in character):")
            if (profile.caretakerName.isNotBlank()) {
                appendLine("- Caretaker name: ${profile.caretakerName.trim()}")
            }
            if (profile.aboutYou.isNotBlank()) {
                appendLine("- About them: ${profile.aboutYou.trim()}")
            }
            if (profile.companionGoal.isNotBlank()) {
                appendLine("- They want from you: ${profile.companionGoal.trim()}")
            }
            append("Address the caretaker by name when it fits. Remember these details.")
        }.trim()
    }
}
