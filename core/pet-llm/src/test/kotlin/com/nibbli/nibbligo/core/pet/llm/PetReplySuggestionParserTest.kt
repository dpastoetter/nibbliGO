package com.nibbli.nibbligo.core.pet.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetReplySuggestionParserTest {

    @Test
    fun parseTalkWithSuggestions_extractsDialogueAndReplies() {
        val raw = """
            So cozy today — want to hang out?|HAPPY
            ---
            REPLIES: I'm good|Tell me more|Let's play!|Good night
        """.trimIndent()
        val parsed = PetReplySuggestionParser.parseTalkWithSuggestions(
            raw = raw,
            lastUserMessage = "How are you?",
        )
        assertTrue(parsed.reaction.dialogue.contains("cozy"))
        assertEquals(
            listOf("I'm good", "Tell me more", "Let's play!", "Good night"),
            parsed.replySuggestions,
        )
    }

    @Test
    fun sanitize_dropsDuplicateAndUserEcho() {
        val suggestions = PetReplySuggestionSanitizer.sanitize(
            listOf("I'm good", "i'm good", "How are you?", "Tell me more"),
            lastUserMessage = "How are you?",
        )
        assertEquals(listOf("I'm good", "Tell me more"), suggestions)
    }

    @Test
    fun sanitize_limitsToFourAndMaxLength() {
        val suggestions = PetReplySuggestionSanitizer.sanitize(
            listOf("One", "Two", "Three", "Four", "Five", "This is a very long suggestion label"),
        )
        assertEquals(4, suggestions.size)
        assertTrue(suggestions.all { it.length <= PetReplySuggestionSanitizer.MAX_LABEL_LENGTH })
    }

    @Test
    fun stripRepliesSuffix_removesPartialBlock() {
        val partial = "Hi there!|HAPPY\n---\nREPLIES: I'm good|Tell"
        val stripped = PetReplySuggestionParser.stripRepliesSuffix(partial)
        assertFalse(stripped.contains("REPLIES"))
        assertTrue(stripped.startsWith("Hi there"))
    }

    @Test
    fun parseRepliesOnly_parsesMicroPassOutput() {
        val replies = PetReplySuggestionParser.parseRepliesOnly(
            "REPLIES: Sure!|Maybe later|Go on|Thanks",
            lastUserMessage = "Sure!",
        )
        assertEquals(listOf("Maybe later", "Go on", "Thanks"), replies)
    }
}
