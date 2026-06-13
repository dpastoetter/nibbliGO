package com.nibbli.nibbligo.core.pet.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionMemoryRendererTest {
    @Test
    fun renderKnownAboutCaretaker_formatsBullets() {
        val block = CompanionMemoryRenderer.renderKnownAboutCaretaker("Likes snacks • Works nights")
        assertTrue(block!!.contains("Known about caretaker:"))
        assertTrue(block.contains("• Likes snacks"))
        assertTrue(block.contains("• Works nights"))
    }

    @Test
    fun renderRecentTurns_skippedWhenEmpty() {
        assertNull(CompanionMemoryRenderer.renderRecentTurns(emptyList()))
    }

    @Test
    fun renderRecentTurns_formatsTranscript() {
        val block = CompanionMemoryRenderer.renderRecentTurns(
            listOf(
                TalkTurnPair("Hi", "Hey there!"),
            ),
        )
        assertTrue(block!!.contains("Recent:"))
        assertTrue(block.contains("Caretaker: Hi"))
        assertTrue(block.contains("You: Hey there!"))
    }

    @Test
    fun parseFacts_dedupesCaseInsensitive() {
        val facts = CompanionMemoryRenderer.parseFacts("Likes tea • likes tea • Reads sci-fi")
        assertEquals(2, facts.size)
    }
}
