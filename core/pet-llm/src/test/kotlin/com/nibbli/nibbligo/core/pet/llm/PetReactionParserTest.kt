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
    fun stripForStreaming_hides_partial_expression_tag() {
        assertEquals("So cozy", PetReactionParser.stripForStreaming("So cozy|HAP"))
        assertEquals("Hello", PetReactionParser.stripForStreaming("Hello"))
        assertEquals("", PetReactionParser.stripForStreaming("|HAPPY"))
    }

    @Test
    fun parseTalk_joins_multiple_lines() {
        val reaction = PetReactionParser.parseTalk("Hello!\nExtra line included.")
        assertEquals("Hello! Extra line included.", reaction.dialogue)
    }

    @Test
    fun parseTalk_expression_suffix() {
        val reaction = PetReactionParser.parseTalk("Doing great!\nNeed a snack.|HUNGRY")
        assertEquals("Doing great! Need a snack.", reaction.dialogue)
        assertEquals(PetExpression.HUNGRY, reaction.suggestedExpression)
    }

    @Test
    fun reconcileTalkStream_keeps_longer_streamed_text() {
        val parsed = PetReactionParser.parseTalk("Hi.")
        val reconciled = PetReactionParser.reconcileTalkStream(parsed, "Hi there, doing well today!")
        assertEquals("Hi there, doing well today!", reconciled.dialogue)
    }

    @Test
    fun stripForStreaming_joins_partial_lines() {
        assertEquals(
            "I'm okay Hunger",
            PetReactionParser.stripForStreaming("I'm okay\nHunger"),
        )
    }

    @Test
    fun sanitizeModelEcho_strips_system_role_prefix() {
        val leaked = "systemYou are a Pixel Friend AI pet in the nibbliGO app."
        assertEquals("", PetReactionParser.sanitizeModelEcho(leaked))
    }

    @Test
    fun sanitizeModelEcho_keeps_reply_after_prompt_echo() {
        val leaked = """
            systemYou are a Pixel Friend AI pet in the nibbliGO app.
            Examples:
            User: How are you?
            You: I'm cheerful!|HAPPY
            User: What's up?
            You: Just vibing on your home screen!|HAPPY
        """.trimIndent()
        assertEquals("Just vibing on your home screen!|HAPPY", PetReactionParser.sanitizeModelEcho(leaked))
    }

    @Test
    fun parseTalk_strips_leaked_system_prompt() {
        val reaction = PetReactionParser.parseTalk(
            "systemYou are a Pixel Friend AI pet in the nibbliGO app. User: hi You: Hey there!|HAPPY",
        )
        assertEquals("Hey there!", reaction.dialogue)
        assertEquals(PetExpression.HAPPY, reaction.suggestedExpression)
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

    @Test
    fun collapseCommaRepetition_fixes_status_echo() {
        val echoed = "I'm content and cozy, content and cozy, content and cozy, content and cozy"
        assertEquals(
            "I'm content and cozy",
            PetReactionParser.collapseCommaRepetition(echoed),
        )
    }

    @Test
    fun parseTalk_collapses_repeated_status_phrases() {
        val reaction = PetReactionParser.parseTalk(
            "I'm content and cozy, content and cozy, content and cozy, content and cozy|HAPPY",
        )
        assertEquals("I'm content and cozy", reaction.dialogue)
    }

    @Test
    fun parseTalk_longReply_notTruncatedAt320() {
        val longBody = "A".repeat(500)
        val reaction = PetReactionParser.parseTalk("$longBody|HAPPY")
        assertEquals(500, reaction.dialogue.length)
    }

    @Test
    fun hasDegenerateRepetition_detects_status_echo() {
        assertTrue(
            PetReactionParser.hasDegenerateRepetition(
                "I'm content and cozy, content and cozy, content and cozy, content and cozy",
            ),
        )
        assertTrue(!PetReactionParser.hasDegenerateRepetition("I'm cozy and hunger is fine."))
    }

    @Test
    fun parseTalk_strips_third_person_nibbli_prefix() {
        val reaction = PetReactionParser.parseTalk("Nibbli, I'm doing great today!|HAPPY")
        assertEquals("I'm doing great today!", reaction.dialogue)
    }

    @Test
    fun parseTalk_rewrites_third_person_pet_name() {
        val reaction = PetReactionParser.parseTalk("Pixel is feeling sleepy.|SLEEPY", petName = "Pixel")
        assertEquals("I'm feeling sleepy.", reaction.dialogue)
    }

    @Test
    fun normalizeFirstPersonDialogue_strips_nibbli_comma() {
        assertEquals(
            "hello there!",
            PetReactionParser.normalizeFirstPersonDialogue("Nibbli, hello there!"),
        )
    }

    @Test
    fun normalizeFirstPersonDialogue_preserves_caretaker_vocative() {
        assertEquals(
            "Nibbli, welcome back!",
            PetReactionParser.normalizeFirstPersonDialogue(
                "Nibbli, welcome back!",
                petName = "Pixel",
                caretakerName = "Nibbli",
            ),
        )
    }

    @Test
    fun parseTalk_preserves_caretaker_vocative_when_caretaker_is_nibbli() {
        val reaction = PetReactionParser.parseTalk(
            "Nibbli, hey there!|HAPPY",
            petName = "Pixel",
            caretakerName = "Nibbli",
        )
        assertEquals("Nibbli, hey there!", reaction.dialogue)
    }
}
