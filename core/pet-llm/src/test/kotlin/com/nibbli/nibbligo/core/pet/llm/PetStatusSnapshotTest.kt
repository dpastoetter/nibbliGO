package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import org.junit.Assert.assertTrue
import org.junit.Test

class PetStatusSnapshotTest {
    @Test
    fun format_includes_low_hunger() {
        val text = PetStatusSnapshot.format(
            PetState(
                stats = PetStats(hunger = 20),
                activeNeed = PetNeed.HUNGRY,
            ),
        )
        assertTrue(text.contains("hunger: 20"))
        assertTrue(text.contains("food"))
    }

    @Test
    fun isStatusQuestion_detects_how_are_you() {
        assertTrue(PetStatusSnapshot.isStatusQuestion("How are you?"))
    }

    @Test
    fun prompt_for_talk_includes_required_reply() {
        val prompt = PetPromptBuilder.build(
            PetReactionRequest(
                state = PetState(stats = PetStats(energy = 15), activeNeed = PetNeed.TIRED),
                userMessage = "How are you?",
            ),
        )
        assertTrue(prompt.contains("required honest reply"))
        assertTrue(prompt.contains("energy: 15"))
    }

    @Test
    fun statusReply_mentions_sleep_when_tired() {
        val reply = com.nibbli.nibbligo.core.model.PetNeedRules.statusReply(
            PetState(stats = PetStats(energy = 12)),
        )
        assertTrue(reply.contains("Sleep", ignoreCase = true))
    }
}
