package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetPromptBuilderTest {
    @Test
    fun buildParts_splits_static_system_and_dynamic_user_for_talk() {
        val request = PetReactionRequest(
            state = PetState(name = "Pixel"),
            userMessage = "How are you?",
        )
        val parts = PetPromptBuilder.buildParts(request, "functiongemma-270m")

        assertTrue(parts.systemInstruction.contains("Pixel Friend AI pet"))
        assertTrue(parts.systemInstruction.contains("Examples:"))
        assertTrue(parts.systemInstruction.contains("Format: dialogue|"))
        assertTrue(!parts.systemInstruction.contains("Pet name: Pixel"))
        assertTrue(parts.userMessage.contains("Pet name: Pixel"))
        assertTrue(parts.userMessage.contains("User: How are you?"))
    }

    @Test
    fun buildParts_compact_profile_puts_status_in_user_turn() {
        val parts = PetPromptBuilder.buildParts(
            PetReactionRequest(state = PetState(), userMessage = "Hi"),
            "functiongemma-270m",
        )
        assertTrue(parts.userMessage.contains("Status: hunger"))
        assertTrue(!parts.systemInstruction.contains("Status: hunger"))
        assertTrue(!parts.userMessage.contains("--- Current status"))
    }

    @Test
    fun buildParts_compact_profile_for_smollm2() {
        val parts = PetPromptBuilder.buildParts(
            PetReactionRequest(state = PetState(), userMessage = "Hi"),
            "smollm2-360m-instruct",
        )
        assertTrue(parts.userMessage.contains("Status: hunger"))
        assertTrue(!parts.systemInstruction.contains("Status: hunger"))
    }

    @Test
    fun buildParts_rich_profile_includes_memory_in_user_turn_for_gemma4() {
        val state = PetState(memorySummary = "Likes snacks")
        val parts = PetPromptBuilder.buildParts(
            PetReactionRequest(state = state, userMessage = "Hey"),
            "gemma-4-e2b-it",
        )
        assertTrue(parts.userMessage.contains("Memory: Likes snacks"))
        assertTrue(parts.systemInstruction.contains("Examples:"))
        assertTrue(!parts.systemInstruction.contains("Likes snacks"))
    }

    @Test
    fun buildParts_mood_pulse_uses_ambient_user_trigger() {
        val parts = PetPromptBuilder.buildParts(
            PetReactionRequest(state = PetState(), moodPulse = true),
            "functiongemma-270m",
        )
        assertTrue(parts.userMessage.contains("spontaneous home-screen thought"))
        assertTrue(parts.systemInstruction.contains("Examples:"))
    }

    @Test
    fun buildCompactTalkParts_uses_static_system_and_compact_user_turn() {
        val parts = PetPromptBuilder.buildCompactTalkParts(
            PetReactionRequest(state = PetState(), userMessage = "How are you?"),
            "smollm2-360m-instruct",
        )
        assertTrue(parts.userMessage.contains("User: How are you?"))
        assertTrue(parts.userMessage.contains("Format: words|HAPPY"))
        assertTrue(parts.systemInstruction.contains("Format: dialogue|"))
        assertTrue(!parts.systemInstruction.contains("Format: words|HAPPY"))
    }

    @Test
    fun buildParts_standard_profile_uses_full_status_snapshot_in_user_turn() {
        val parts = PetPromptBuilder.buildParts(
            PetReactionRequest(state = PetState(), userMessage = "Hi"),
            "some-other-model",
        )
        assertTrue(parts.userMessage.contains("--- Current status"))
        assertTrue(!parts.systemInstruction.contains("--- Current status"))
    }

    @Test
    fun static_system_instruction_is_stable_across_different_stats() {
        val modelId = "smollm2-360m-instruct"
        val lowHunger = PetState(stats = PetState().stats.copy(hunger = 10))
        val highHunger = PetState(stats = PetState().stats.copy(hunger = 90))
        val sysA = PetPromptBuilder.buildParts(
            PetReactionRequest(state = lowHunger, userMessage = "Hi"),
            modelId,
        ).systemInstruction
        val sysB = PetPromptBuilder.buildParts(
            PetReactionRequest(state = highHunger, userMessage = "Hi"),
            modelId,
        ).systemInstruction
        assertEquals(sysA, sysB)
    }
}
