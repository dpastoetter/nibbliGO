package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetState
import org.junit.Assert.assertTrue
import org.junit.Test

class PetMemoryWriterTest {
    @Test
    fun appendFact_caps_length() {
        var memory = ""
        repeat(10) { i ->
            memory = PetMemoryWriter.appendFact(memory, "fact number $i is fairly long")
        }
        assertTrue(memory.length <= 400)
    }

    @Test
    fun factFromReaction_skips_mood_pulse() {
        val fact = PetMemoryWriter.factFromReaction(
            PetReactionRequest(state = PetState(), moodPulse = true),
            "Hello",
        )
        assertTrue(fact == null)
    }

    @Test
    fun withNewFact_updates_state() {
        val state = PetState()
        val updated = PetMemoryWriter.withNewFact(
            state,
            PetReactionRequest(state = state, userMessage = "hi"),
            "Hey there!",
        )
        assertTrue(updated.memorySummary.isNotBlank())
    }
}
