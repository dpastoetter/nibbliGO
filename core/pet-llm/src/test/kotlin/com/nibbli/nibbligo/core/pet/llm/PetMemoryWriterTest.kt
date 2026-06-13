package com.nibbli.nibbligo.core.pet.llm

import org.junit.Assert.assertTrue
import org.junit.Test

class PetMemoryWriterTest {
    @Test
    fun appendFact_caps_length() {
        var memory = ""
        repeat(10) { i ->
            memory = PetMemoryWriter.appendFact(memory, "fact number $i is fairly long")
        }
        assertTrue(memory.length <= CompanionMemoryRenderer.MAX_RENDER_CHARS)
    }

    @Test
    fun proposeUserFact_detectsPersonalLine() {
        val fact = PetMemoryWriter.proposeUserFact("I'm studying Kotlin this week")
        assertTrue(fact?.contains("Kotlin") == true)
    }

    @Test
    fun proposeUserFact_ignoresShortChitChat() {
        assertTrue(PetMemoryWriter.proposeUserFact("ok cool") == null)
    }
}
