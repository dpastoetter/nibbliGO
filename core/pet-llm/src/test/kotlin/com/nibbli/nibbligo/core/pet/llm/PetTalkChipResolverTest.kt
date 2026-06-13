package com.nibbli.nibbligo.core.pet.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class PetTalkChipResolverTest {

    @Test
    fun resolve_startersBeforeFirstTurn() {
        assertEquals(
            PetTalkSuggestions.starterChips,
            PetTalkChipResolver.resolve(lastTurn = null, isGenerating = false),
        )
    }

    @Test
    fun resolve_emptyWhileGenerating() {
        val turn = LastTalkTurn("Hi", "Hello!", listOf("Tell me more"), 0L)
        assertEquals(emptyList<String>(), PetTalkChipResolver.resolve(turn, isGenerating = true))
    }

    @Test
    fun resolve_llmSuggestionsAfterTurn() {
        val turn = LastTalkTurn(
            userMessage = "Hi",
            petDialogue = "Hey there!",
            replySuggestions = listOf("I'm good", "Tell me more"),
            timestampMillis = 0L,
        )
        assertEquals(
            listOf("I'm good", "Tell me more"),
            PetTalkChipResolver.resolve(turn, isGenerating = false),
        )
    }
}
