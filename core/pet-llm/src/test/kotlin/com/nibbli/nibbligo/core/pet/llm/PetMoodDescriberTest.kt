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
            modelId = "functiongemma-270m",
        )
        assertTrue(prompt.contains("Mood:"))
        assertTrue(prompt.contains("spontaneous home-screen thought"))
    }

    @Test
    fun templateLine_never_blank() {
        val line = PetMoodDescriber.templateLine(PetState())
        assertTrue(line.isNotBlank())
    }

    @Test
    fun prompt_includes_recent_lines_and_activity_hint() {
        val prompt = PetPromptBuilder.build(
            PetReactionRequest(
                state = PetState(memorySummary = "Likes snacks."),
                recentLines = listOf("Earlier beep.", "Second line."),
                activityHint = "They finished an agent task.",
            ),
            modelId = "gemma-4-e2b-it",
        )
        assertTrue(prompt.contains("You recently said:"))
        assertTrue(prompt.contains("Earlier beep."))
        assertTrue(prompt.contains("React briefly in character to: They finished an agent task."))
        assertTrue(prompt.contains("Memory: Likes snacks."))
    }
}
