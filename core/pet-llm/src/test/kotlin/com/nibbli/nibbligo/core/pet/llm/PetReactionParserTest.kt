package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetState
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
    fun parse_expression_suffix() {
        val reaction = PetReactionParser.parse("So cozy!|HAPPY")
        assertEquals("So cozy!", reaction.dialogue)
        assertEquals(PetExpression.HAPPY, reaction.suggestedExpression)
    }

    @Test
    fun splitDialogueAndExpression_invalid_tag_returns_null_expression() {
        val (text, expr) = PetReactionParser.splitDialogueAndExpression("Hi|NOTREAL")
        assertEquals("Hi", text)
        assertEquals(null, expr)
    }

    @Test
    fun fallback_never_empty() {
        val reaction = PetReactionParser.fallback(
            PetReactionRequest(
                state = PetState(),
                userMessage = "hi",
            ),
        )
        assertTrue(reaction.dialogue.isNotBlank())
    }
}
