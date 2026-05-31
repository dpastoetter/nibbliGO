package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetOnboardingProfile
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetOnboardingPromptTest {
    @Test
    fun formatForSystemInstruction_emptyProfile_returnsNull() {
        assertNull(PetOnboardingPrompt.formatForSystemInstruction(PetOnboardingProfile()))
    }

    @Test
    fun formatForSystemInstruction_includesCaretakerDetails() {
        val block = PetOnboardingPrompt.formatForSystemInstruction(
            PetOnboardingProfile(
                caretakerName = "Alex",
                aboutYou = "Loves retro tech",
                companionGoal = "A cozy home-screen buddy",
                completed = true,
            ),
            petName = "Pixel",
        ).orEmpty()
        assertTrue(block.contains("You are Pixel"))
        assertTrue(block.contains("Alex"))
        assertTrue(block.contains("retro tech"))
        assertTrue(block.contains("cozy home-screen buddy"))
        assertTrue(block.contains("Never refer to yourself by name in third person"))
    }

    @Test
    fun formatForSystemInstruction_customPetName_withoutProfile_returnsIdentityBlock() {
        val block = PetOnboardingPrompt.formatForSystemInstruction(
            PetOnboardingProfile(),
            petName = "Pixel",
        ).orEmpty()
        assertTrue(block.contains("You are Pixel"))
    }

    @Test
    fun homeTalkSystemInstruction_includesOnboardingBlock() {
        val context = PetOnboardingPrompt.formatForSystemInstruction(
            PetOnboardingProfile(caretakerName = "Sam", completed = true),
            petName = "Buddy",
        )
        val rules = PetPromptBuilder.homeTalkSystemInstruction(context, "Buddy")
        assertTrue(rules.contains("Caretaker name: Sam"))
        assertTrue(rules.contains("You are Buddy"))
    }
}
