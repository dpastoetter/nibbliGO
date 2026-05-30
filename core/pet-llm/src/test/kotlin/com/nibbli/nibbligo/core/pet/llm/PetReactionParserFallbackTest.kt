package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetReactionParserFallbackTest {

    @Test
    fun fallback_statusQuestion_usesNeedRules() {
        val request = PetReactionRequest(
            state = PetState(),
            userMessage = "How are you?",
        )
        val reaction = PetReactionParser.fallback(request)
        assertTrue(reaction.dialogue.isNotBlank())
        assertTrue(reaction.dialogue != "I got distracted… try talking again?")
    }

    @Test
    fun fallback_gameQuestion_usesFaqAnswer() {
        val request = PetReactionRequest(
            state = PetState(),
            userMessage = "How do I evolve?",
        )
        val reaction = PetReactionParser.fallback(request)
        assertTrue(reaction.dialogue.contains("Evolution"))
    }

    @Test
    fun fallback_genericUserMessage_mentionsHiccup() {
        val request = PetReactionRequest(
            state = PetState(),
            userMessage = "Hello!",
        )
        val reaction = PetReactionParser.fallback(request)
        assertEquals(
            "Hmm, my on-device brain hiccuped — ask again in a sec?",
            reaction.dialogue,
        )
    }
}
