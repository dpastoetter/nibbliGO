package com.nibbli.nibbligo.core.pet.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetReactionParserTest {
    @Test
    fun parse_takes_first_line() {
        val reaction = PetReactionParser.parse("Hello!\nExtra line ignored.")
        assertEquals("Hello!", reaction.dialogue)
    }

    @Test
    fun fallback_never_empty() {
        val reaction = PetReactionParser.fallback(
            PetReactionRequest(
                state = com.nibbli.nibbligo.core.model.PetState(),
                userMessage = "hi",
            ),
        )
        assertTrue(reaction.dialogue.isNotBlank())
    }
}
