package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetNeedRules
import com.nibbli.nibbligo.core.model.PetState

/** Plain-language snapshot of pet stats for LLM talk replies. */
object PetStatusSnapshot {
    fun format(state: PetState, nowMillis: Long = System.currentTimeMillis()): String = buildString {
        val stats = state.stats
        val need = PetNeedRules.deriveNeed(state, nowMillis)
        appendLine("--- Current status (stats 0–100, higher is better unless noted) ---")
        appendLine("hunger: ${stats.hunger}, happiness: ${stats.happiness}, energy: ${stats.energy}")
        appendLine("hygiene: ${stats.hygiene}, health: ${stats.health}, trust: ${stats.trust}")
        appendLine("stage: ${state.stage.name}, condition: ${state.condition.name}")
        appendLine("simulated active need: ${need.name}, stored active need: ${state.activeNeed.name}")
        appendLine("expression: ${state.expression.name}")
        if (state.hasMess) appendLine("room: messy (needs Clean)")
        appendLine("mood summary: ${PetMoodDescriber.describe(state)}")
        append("needs right now: ")
        appendLine(formatNeeds(state, nowMillis))
        appendLine("required honest reply: ${PetNeedRules.statusReply(state, nowMillis)}")
    }

    fun formatNeeds(state: PetState, nowMillis: Long = System.currentTimeMillis()): String {
        if (state.condition == PetCondition.DEAD) return "none (pet is gone)"
        if (state.condition == PetCondition.SLEEPING) return "sleep (use Wake when ready)"

        val need = PetNeedRules.deriveNeed(state, nowMillis)
        val stats = state.stats
        val hints = buildList {
            when (need) {
                PetNeed.HUNGRY -> add("food — hunger ${stats.hunger}/100 (Meal or Snack)")
                PetNeed.DIRTY -> add("cleaning — hygiene ${stats.hygiene}/100 (Clean)")
                PetNeed.TIRED -> add("rest — energy ${stats.energy}/100 (Sleep)")
                PetNeed.SICK -> add("medicine — health ${stats.health}/100 (Meds)")
                PetNeed.UNHAPPY -> add("cheer up — mood ${stats.mood}/100 (Play or Talk)")
                PetNeed.LONELY -> add("company — lonely (Talk or Play)")
                PetNeed.NONE -> Unit
            }
        }

        return if (hints.isEmpty()) {
            "nothing urgent — hunger ${stats.hunger}, mood ${stats.mood}, energy ${stats.energy}"
        } else {
            hints.joinToString("; ")
        }
    }

    fun isStatusQuestion(message: String): Boolean {
        val lower = message.lowercase().trim()
        return STATUS_PHRASES.any { lower.contains(it) }
    }

    private val STATUS_PHRASES = listOf(
        "how are you",
        "how do you feel",
        "how you doing",
        "what do you need",
        "what do you want",
        "are you ok",
        "are you okay",
        "you okay",
        "what's wrong",
        "whats wrong",
        "how's it going",
        "hows it going",
    )
}
