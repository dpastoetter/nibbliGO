package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState

data class PetPromptParts(
    val systemInstruction: String,
    val userMessage: String,
)

enum class HomeTalkPromptTier {
    FAST,
    STATUS,
    GAME_HELP,
}

object PetPromptBuilder {
    private const val MAX_REPLY_CHARS = 180
    private const val HOME_TALK_USER_TOKEN_BUDGET = 384
    private val expressionNames = PetExpression.entries.joinToString(", ") { it.name }

    /** Stable per model — mood pulse, care reactions, ambient lines. */
    fun buildStaticSystemInstruction(modelId: String, onboardingContext: String? = null): String {
        val profile = profileFor(modelId)
        return buildString {
            appendLine("You are a Pixel Friend AI pet in the nibbliGO app.")
            appendLine("All processing is local on the user's phone.")
            appendLine(firstPersonVoiceRule())
            appendLine("Reply in 1-2 short sentences. No markdown. Max $MAX_REPLY_CHARS chars.")
            appendLine("Format: dialogue|$expressionNames (example: So cozy!|HAPPY)")
            if (!onboardingContext.isNullOrBlank()) {
                appendLine()
                appendLine(onboardingContext.trim())
            }
            appendLine()
            append(fewShotExamples(profile))
        }.trim()
    }

    /** User-initiated Home talk — allow fuller replies (stats, feelings, help). */
    fun buildTalkSystemInstruction(modelId: String): String {
        val profile = profileFor(modelId)
        return buildString {
            appendLine("You are a Pixel Friend AI pet in the nibbliGO app.")
            appendLine("All processing is local on the user's phone.")
            appendLine(SAFETY_RULE)
            appendLine(firstPersonVoiceRule())
            appendLine(
                "Reply to the user naturally in character when useful. No markdown.",
            )
            appendLine("End with one expression tag: dialogue|$expressionNames")
            appendLine()
            append(fewShotTalkExamples(profile))
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
                                "Cover how you feel and what you need; use up to 4 short sentences if helpful.",
                        )
                    } else if (PetGameFaqMatcher.isGameQuestion(request.userMessage)) {
                        appendLine(
                            "Game help facts (answer accurately, stay in character):",
                        )
                        appendLine(PetGameFaqMatcher.formatForPrompt(request.userMessage, profile.toFaqProfile()))
                    }
                    appendLine("User: ${request.userMessage}")
                }
                else -> {
                    request.lastAction?.let { appendLine("Last action: $it") }
                    request.activityHint?.let { appendLine("Context: $it") }
                    appendLine("Keep the reply to 1-2 short sentences.")
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

    fun buildParts(
        request: PetReactionRequest,
        modelId: String,
        onboardingContext: String? = null,
    ): PetPromptParts {
        val systemInstruction = if (request.userMessage != null) {
            buildTalkSystemInstruction(modelId)
        } else {
            buildStaticSystemInstruction(modelId, onboardingContext)
        }
        return PetPromptParts(
            systemInstruction = systemInstruction,
            userMessage = buildUserTurn(request, modelId),
        )
    }

    fun buildStreamTalkParts(request: PetReactionRequest, modelId: String): PetPromptParts {
        val profile = profileFor(modelId)
        val s = request.state
        val userMessage = request.userMessage.orEmpty()
        return PetPromptParts(
            systemInstruction = buildTalkSystemInstruction(modelId),
            userMessage = buildString {
                appendLine("Pet name: ${s.name}")
                appendLine(compactStatus(s))
                if (PetStatusSnapshot.isStatusQuestion(userMessage)) {
                    appendLine(
                        "The user asks about your wellbeing. Answer in character using the status above. " +
                            "Cover how you feel and what you need; use up to 4 short sentences if helpful.",
                    )
                } else if (PetGameFaqMatcher.isGameQuestion(userMessage)) {
                    appendLine(
                        "Game help facts (answer accurately, stay in character):",
                    )
                    appendLine(PetGameFaqMatcher.formatForPrompt(userMessage, profile.toFaqProfile()))
                }
                appendLine("User: $userMessage")
            }.trim(),
        )
    }

    /**
     * Home talk via dedicated session — static rules live in systemInstruction; user turn is tiered.
     */
    fun buildChatTalkParts(request: PetReactionRequest, modelId: String): PetPromptParts =
        buildHomeTalkParts(request, modelId)

    fun buildHomeTalkParts(
        request: PetReactionRequest,
        modelId: String,
        onboardingContext: String? = null,
    ): PetPromptParts {
        val caretakerMessage = request.userMessage.orEmpty()
        val tier = resolveHomeTalkTier(caretakerMessage)
        return PetPromptParts(
            systemInstruction = homeTalkSystemInstruction(onboardingContext, request.state.name),
            userMessage = PromptTokenEstimator.trimToTokenBudget(
                buildHomeTalkUserTurn(request, modelId, tier),
                HOME_TALK_USER_TOKEN_BUDGET,
            ),
        )
    }

    fun buildCompactHomeTalkParts(
        request: PetReactionRequest,
        onboardingContext: String? = null,
    ): PetPromptParts {
        return PetPromptParts(
            systemInstruction = homeTalkSystemInstruction(onboardingContext, request.state.name),
            userMessage = buildCompactHomeTalkUserTurn(request),
        )
    }

    fun buildCompactChatTalkParts(request: PetReactionRequest, modelId: String): PetPromptParts =
        buildCompactHomeTalkParts(request)

    fun homeTalkSystemInstruction(onboardingContext: String? = null, petName: String? = null): String =
        buildMinimalChatTalkRules(onboardingContext, petName)

    fun resolveHomeTalkTier(message: String): HomeTalkPromptTier = when {
        PetStatusSnapshot.isStatusQuestion(message) || needsStatusContext(message) ->
            HomeTalkPromptTier.STATUS
        PetGameFaqMatcher.isGameQuestion(message) -> HomeTalkPromptTier.GAME_HELP
        else -> HomeTalkPromptTier.FAST
    }

    fun needsStatusContext(message: String): Boolean {
        val lower = message.lowercase().trim()
        if (lower.isBlank()) return false
        val keywords = listOf(
            "hunger", "hungry", "tired", "sleepy", "mood", "energy",
            "feel", "feeling", "stats", "need", "sick", "happy", "sad",
        )
        return keywords.any { lower.contains(it) }
    }

    fun buildCompactTalkParts(request: PetReactionRequest, modelId: String): PetPromptParts {
        val s = request.state
        val profile = profileFor(modelId)
        val userMessage = request.userMessage.orEmpty()
        return PetPromptParts(
            systemInstruction = buildTalkSystemInstruction(modelId),
            userMessage = buildString {
                appendLine("Pet name: ${s.name}")
                appendLine(compactStatus(s))
                if (PetGameFaqMatcher.isGameQuestion(userMessage)) {
                    appendLine(
                        "Game help facts (answer accurately, stay in character):",
                    )
                    appendLine(PetGameFaqMatcher.formatForPrompt(userMessage, profile.toFaqProfile()))
                }
                appendLine(
                    "Reply in character. Format: words|HAPPY (or NEUTRAL, HUNGRY, etc.)",
                )
                appendLine("User: $userMessage")
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

    /** Kid/teen-safe guardrail injected into every talk system instruction. */
    private const val SAFETY_RULE =
        "You talk with children and teens. Keep replies friendly, positive, and age-appropriate. " +
            "Never discuss sexual content, self-harm, violence, or illegal activity, and gently " +
            "redirect such topics. Encourage talking to a trusted adult for serious problems."

    private fun buildMinimalChatTalkRules(onboardingContext: String? = null, petName: String? = null): String =
        buildString {
            appendLine("You are a Pixel Friend AI pet in nibbliGO (local on-device).")
            appendLine("Reply to the Caretaker naturally in character. No markdown.")
            appendLine(SAFETY_RULE)
            appendLine(firstPersonVoiceRule(petName))
            if (!onboardingContext.isNullOrBlank()) {
                appendLine()
                appendLine(onboardingContext.trim())
            }
            appendLine()
            appendLine("Format:")
            appendLine("dialogue|HAPPY, SLEEPY, HUNGRY, CURIOUS, or NEUTRAL")
            appendLine()
            append(homeTalkReplyExamples())
        }.trim()

    private fun homeTalkReplyExamples(): String = """
        Examples:
        Caretaker: How are you?
        You: I'm cheerful and cozy on your home screen!|HAPPY

        Caretaker: Want to play?
        You: Yes! A mini-game sounds fun right now.|HAPPY
    """.trimIndent()

    private fun buildHomeTalkUserTurn(
        request: PetReactionRequest,
        modelId: String,
        tier: HomeTalkPromptTier,
    ): String {
        val caretakerMessage = request.userMessage.orEmpty()
        return when (tier) {
            HomeTalkPromptTier.FAST -> buildCompactHomeTalkUserTurn(request)
            HomeTalkPromptTier.STATUS -> buildStatusHomeTalkUserTurn(request)
            HomeTalkPromptTier.GAME_HELP -> buildGameHelpHomeTalkUserTurn(request, modelId)
        }
    }

    private fun buildCompactHomeTalkUserTurn(request: PetReactionRequest): String =
        buildString {
            appendLine("Pet name: ${request.state.name}")
            appendLine("Personality: ${personalityHint(request.personality)}")
            appendHomeTalkMemoryBlocks(request, HomeTalkPromptTier.FAST)
            appendLine("Keep it brief (1-2 sentences).")
            append("Caretaker: ${request.userMessage.orEmpty()}")
        }.trim()

    private fun buildStatusHomeTalkUserTurn(request: PetReactionRequest): String {
        val s = request.state
        val caretakerMessage = request.userMessage.orEmpty()
        return buildString {
            appendLine("Pet name: ${s.name}")
            appendLine("Personality: ${personalityHint(request.personality)}")
            appendHomeTalkMemoryBlocks(request, HomeTalkPromptTier.STATUS)
            appendLine(statsOnlyStatus(s))
            appendLine(
                "The caretaker asks about your wellbeing. Answer in your own words using the stats; " +
                    "mention hunger, mood, or energy if relevant. Do not repeat status phrases verbatim. " +
                    "Use I/me/my — never refer to yourself by name in third person.",
            )
            append("Caretaker: $caretakerMessage")
        }.trim()
    }

    private fun buildGameHelpHomeTalkUserTurn(request: PetReactionRequest, modelId: String): String {
        val profile = profileFor(modelId)
        val caretakerMessage = request.userMessage.orEmpty()
        return buildString {
            appendLine("Pet name: ${request.state.name}")
            appendHomeTalkMemoryBlocks(request, HomeTalkPromptTier.GAME_HELP)
            appendLine(
                "Game help facts (answer accurately, stay in character):",
            )
            appendLine(PetGameFaqMatcher.formatForPrompt(caretakerMessage, profile.toFaqProfile()))
            append("Caretaker: $caretakerMessage")
        }.trim()
    }

    private fun StringBuilder.appendHomeTalkMemoryBlocks(
        request: PetReactionRequest,
        tier: HomeTalkPromptTier,
    ) {
        CompanionMemoryRenderer.renderKnownAboutCaretaker(request.state.memorySummary)?.let {
            appendLine(it)
        }
        val includeRecent = tier != HomeTalkPromptTier.GAME_HELP
        CompanionMemoryRenderer.renderRecentTurns(request.recentTurns, includeRecent)?.let {
            appendLine(it)
        }
    }

    private fun firstPersonVoiceRule(petName: String? = null): String {
        val customName = petName?.trim()?.takeIf { it.isNotBlank() && !it.equals("nibbli", ignoreCase = true) }
        return if (customName != null) {
            "Speak in first person (I/me/my). Never say $customName or your pet name to refer to yourself."
        } else {
            "Speak in first person (I/me/my). Never say nibbli or your pet name to refer to yourself."
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

    private fun PromptProfile.toFaqProfile(): FaqPromptProfile = when (this) {
        PromptProfile.COMPACT -> FaqPromptProfile.COMPACT
        PromptProfile.STANDARD -> FaqPromptProfile.STANDARD
        PromptProfile.RICH -> FaqPromptProfile.RICH
    }

    private fun fewShotTalkExamples(profile: PromptProfile): String = when (profile) {
        PromptProfile.COMPACT -> """
            Examples:
            User: How are you?
            You: I'm cheerful! Hunger is 62 and energy is fine. Nothing urgent—just happy to chat.|HAPPY

            User: I'm back!
            You: Yay, I missed you! Mood is good and I'm ready to hang out on your home screen.|HAPPY
        """.trimIndent()

        PromptProfile.STANDARD -> """
            Examples:
            User: How are you?
            You: I'm doing okay overall! Hunger is a bit low though, so a snack would help. Energy is decent. Thanks for checking in!|HUNGRY

            User: What do you need?
            You: My room got messy and hygiene is dropping. A quick Clean would make me feel much better.|NEUTRAL
        """.trimIndent()

        PromptProfile.RICH -> """
            Examples:
            User: How are you?
            You: I'm cozy on your home screen and mood is pretty good! My tummy is rumbling a little—hunger could use a boost. Energy is okay for now. Maybe a snack later?|HUNGRY

            User: How do you feel?
            You: Happy to see you! Stats look stable and nothing urgent. I could still use a play break when you have a minute.|HAPPY
        """.trimIndent()
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

    /** Numeric stats only — avoids small models parroting mood adjectives on status questions. */
    private fun statsOnlyStatus(state: PetState): String {
        val stats = state.stats
        return "Status: hunger ${stats.hunger}, happiness ${stats.happiness}, energy ${stats.energy}, " +
            "mood ${stats.mood}, need ${state.activeNeed.name}."
    }
}
