package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState

data class PetPromptParts(
    val systemInstruction: String,
    val userMessage: String,
)

object PetPromptBuilder {
    private const val MAX_REPLY_CHARS = 180
    private val expressionNames = PetExpression.entries.joinToString(", ") { it.name }

    /** Stable per model — reused across turns for a warm LiteRT pet session. */
    fun buildStaticSystemInstruction(modelId: String): String {
        val profile = profileFor(modelId)
        return buildString {
            appendLine("You are a Pixel Friend AI pet in the nibbliGO app.")
            appendLine("All processing is local on the user's phone.")
            appendLine("Reply in 1-2 short sentences. No markdown. Max $MAX_REPLY_CHARS chars.")
            appendLine("Format: dialogue|$expressionNames (example: So cozy!|HAPPY)")
            appendLine()
            append(fewShotExamples(profile))
        }.trim()
    }

    /** Per-turn context sent as the user message (stats, personality, trigger). */
    fun buildUserTurn(request: PetReactionRequest, modelId: String): String {
        val profile = profileFor(modelId)
        val s = request.state
        return buildString {
            appendLine("Pet name: ${s.name}")
            appendLine("Personality: ${personalityHint(request.personality)}")
            appendLine("Stage: ${s.stage.name}, condition: ${s.condition.name}.")
            if (s.memorySummary.isNotBlank() && profile != PromptProfile.COMPACT) {
                appendLine("Memory: ${s.memorySummary}")
            }
            val recent = request.recentLines.filter { it.isNotBlank() }.distinct().takeLast(2)
            if (recent.isNotEmpty() && profile == PromptProfile.RICH) {
                appendLine("You recently said:")
                recent.forEach { appendLine("- $it") }
            }
            when {
                request.moodPulse -> {
                    appendLine("Mood: ${PetMoodDescriber.describe(s)}")
                    appendLine(
                        "Task: spontaneous home-screen thought. One cute line, no markdown, " +
                            "no question needing an answer.",
                    )
                    appendLine("User: Say a spontaneous home-screen thought.")
                }
                request.userMessage != null -> {
                    appendLine(if (profile == PromptProfile.COMPACT) compactStatus(s) else PetStatusSnapshot.format(s))
                    if (PetStatusSnapshot.isStatusQuestion(request.userMessage)) {
                        appendLine(
                            "The user asks about your wellbeing. Answer in character using the status above. " +
                                "Mention how you feel and what you need if any stat is low.",
                        )
                    }
                    appendLine("User: ${request.userMessage}")
                }
                else -> {
                    request.lastAction?.let { appendLine("Last action: $it") }
                    request.activityHint?.let { appendLine("Context: $it") }
                    appendLine(
                        when {
                            request.activityHint != null ->
                                "User: React briefly in character to: ${request.activityHint}"
                            request.lastAction != null -> "User: User just ${request.lastAction}."
                            else -> "User: Say a short greeting to your caretaker."
                        },
                    )
                }
            }
        }.trim()
    }

    fun buildParts(request: PetReactionRequest, modelId: String): PetPromptParts {
        return PetPromptParts(
            systemInstruction = buildStaticSystemInstruction(modelId),
            userMessage = buildUserTurn(request, modelId),
        )
    }

    /** Shorter user turn when the primary path returns an empty model response. */
    fun buildCompactTalkParts(request: PetReactionRequest, modelId: String): PetPromptParts {
        val s = request.state
        return PetPromptParts(
            systemInstruction = buildStaticSystemInstruction(modelId),
            userMessage = buildString {
                appendLine("Pet name: ${s.name}")
                appendLine(compactStatus(s))
                appendLine(
                    "Reply in one short in-character sentence about how you feel and what you need. " +
                        "Max 100 characters. Format: words|HAPPY (or NEUTRAL, HUNGRY, etc.)",
                )
                appendLine("User: ${request.userMessage.orEmpty()}")
            }.trim(),
        )
    }

    /** Legacy single-string prompt (tests / debugging). */
    fun build(request: PetReactionRequest, modelId: String = ""): String {
        val parts = buildParts(request, modelId)
        return buildString {
            appendLine(parts.systemInstruction)
            appendLine()
            append(parts.userMessage)
        }
    }

    private fun personalityHint(personality: PetPersonality): String = when (personality) {
        PetPersonality.PLAYFUL -> "Playful and bouncy, like a Pixel Friend."
        PetPersonality.CALM -> "Calm and gentle."
        PetPersonality.CURIOUS -> "Curious about on-device AI."
    }

    private fun profileFor(modelId: String): PromptProfile = when {
        modelId.contains("functiongemma", ignoreCase = true) -> PromptProfile.COMPACT
        modelId.contains("smollm", ignoreCase = true) -> PromptProfile.COMPACT
        modelId.contains("gemma-4", ignoreCase = true) -> PromptProfile.RICH
        modelId.contains("gemma3", ignoreCase = true) -> PromptProfile.STANDARD
        modelId.contains("qwen", ignoreCase = true) -> PromptProfile.STANDARD
        modelId.contains("deepseek", ignoreCase = true) -> PromptProfile.STANDARD
        else -> PromptProfile.STANDARD
    }

    private enum class PromptProfile {
        COMPACT,
        STANDARD,
        RICH,
    }

    private fun fewShotExamples(profile: PromptProfile): String = when (profile) {
        PromptProfile.COMPACT -> """
            Examples:
            User: How are you?
            You: A bit hungry—snack time?|HUNGRY

            User: I'm back!
            You: Yay, I missed you!|HAPPY
        """.trimIndent()

        PromptProfile.STANDARD -> """
            Examples:
            User: How are you?
            You: Kinda hungry and sleepy—food and a nap would help!|HUNGRY

            User: What do you need?
            You: My room is messy—could you clean up?|NEUTRAL
        """.trimIndent()

        PromptProfile.RICH -> """
            Examples:
            User: How are you?
            You: I'm cheerful but my tummy's rumbling—maybe a snack?|HUNGRY

            User: How do you feel?
            You: Cozy on your home screen, though I could use a play break!|HAPPY
        """.trimIndent()
    }

    private fun compactStatus(state: PetState): String {
        val stats = state.stats
        return "Status: hunger ${stats.hunger}, happiness ${stats.happiness}, energy ${stats.energy}, " +
            "need ${state.activeNeed.name}. ${PetMoodDescriber.describe(state)}"
    }
}
