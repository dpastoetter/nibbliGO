package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import org.junit.Assert.assertTrue
import org.junit.Test

class PetMoodDescriberTest {
    @Test
    fun describe_includes_sleepy_when_sleeping() {
        val text = PetMoodDescriber.describe(
            PetState(condition = PetCondition.SLEEPING),
        )
        assertTrue(text.contains("sleepy"))
    }

    @Test
    fun moodPulse_prompt_mentions_mood() {
        val prompt = PetPromptBuilder.build(
            PetReactionRequest(
                state = PetState(stats = PetStats(mood = 90)),
                moodPulse = true,
            ),
        )
        assertTrue(prompt.contains("Mood right now:"))
        assertTrue(prompt.contains("Spontaneous thought"))
    }

    @Test
    fun templateLine_never_blank() {
        val line = PetMoodDescriber.templateLine(PetState())
        assertTrue(line.isNotBlank())
    }
}
