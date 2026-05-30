package com.nibbli.nibbligo.core.pet.llm

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetGameFaqMatcherTest {

    @Test
    fun isGameQuestion_detects_mechanics_not_status() {
        assertTrue(PetGameFaqMatcher.isGameQuestion("How do I evolve?"))
        assertTrue(PetGameFaqMatcher.isGameQuestion("What is trust?"))
        assertTrue(PetGameFaqMatcher.isGameQuestion("How do I take care of you?"))
        assertFalse(PetGameFaqMatcher.isGameQuestion("How are you?"))
        assertFalse(PetGameFaqMatcher.isGameQuestion("I'm back!"))
    }

    @Test
    fun select_ranks_evolution_entry_for_evolve_question() {
        val picked = PetGameFaqMatcher.select("How do I evolve?", maxEntries = 2, maxChars = 500)
        assertTrue(picked.any { it.id == "evolution" })
    }

    @Test
    fun select_respects_char_budget_for_compact() {
        val text = PetGameFaqMatcher.formatForPrompt(
            "How do I evolve?",
            FaqPromptProfile.COMPACT,
        )
        assertTrue(text.length <= 250)
        assertTrue(text.contains("Evolution"))
    }

    @Test
    fun bestAnswer_returns_care_basics_for_generic_play_question() {
        val answer = PetGameFaqMatcher.bestAnswer("How do I take care of you?")
        assertTrue(answer!!.contains("Feed Meal"))
    }
}
