package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetNeedRules

object PetReactionParser {
    private const val MAX_LEN = 180
    const val MAX_TALK_LEN = 320
    private val LEADING_ROLE_WORD = Regex("""(?i)^(?:system|user|assistant|model)\s*""")
    private val SYSTEM_ROLE_GLUE = Regex("""(?i)^system(?=You are)""")
    private val FEW_SHOT_REPLY = Regex("""(?m)^You:\s""")
    private val USER_TURN = Regex("""(?m)^User:\s""")
    private val CARETAKER_TURN = Regex("""(?m)^Caretaker:\s""")
    private val INLINE_FEW_SHOT_REPLY = Regex("""You:\s""")
    private val INLINE_USER_TURN = Regex("""User:\s""")
    private const val SYSTEM_MARKER = "You are a Pixel Friend AI pet"

    fun parse(raw: String): PetReaction {
        val primary = normalizedLines(raw).firstOrNull() ?: normalizeRaw(raw)
        return reactionFromDialogue(primary, MAX_LEN)
    }

    /** User talk replies — keep all streamed lines, aligned with [stripForStreaming]. */
    fun parseTalk(raw: String): PetReaction {
        val combined = normalizedLines(raw).joinToString(" ").ifBlank { normalizeRaw(raw) }
        return reactionFromDialogue(combined, MAX_TALK_LEN)
    }

    fun reconcileTalkStream(parsed: PetReaction, streamedText: String): PetReaction {
        val streamed = stripForStreaming(streamedText).take(MAX_TALK_LEN)
        if (streamed.isBlank()) return parsed
        return if (streamed.length > parsed.dialogue.length) {
            parsed.copy(dialogue = streamed)
        } else {
            parsed
        }
    }

    private fun reactionFromDialogue(line: String, maxLen: Int): PetReaction {
        val (dialogue, expression) = splitDialogueAndExpression(line)
        val cleanDialogue = dialogue.take(maxLen).ifBlank { "…" }
        return PetReaction(
            dialogue = cleanDialogue,
            suggestedExpression = expression,
        )
    }

    /** Strips chat-template role markers and leaked pet system prompt from model output. */
    fun sanitizeModelEcho(raw: String): String {
        var text = raw.trim().removeSurrounding("\"").trim()
        while (true) {
            val before = text
            text = LEADING_ROLE_WORD.replace(text, "")
            text = SYSTEM_ROLE_GLUE.replace(text, "")
            if (text == before) break
        }
        if (text.contains(SYSTEM_MARKER, ignoreCase = true) ||
            text.contains("Examples:", ignoreCase = true) ||
            text.contains("Caretaker:", ignoreCase = true)
        ) {
            text = extractReplyAfterPromptEcho(text)
        }
        return text.removePrefix("You:").trimStart().trim()
    }

    fun hasPromptEcho(raw: String): Boolean {
        val text = raw.trim()
        return text.contains(SYSTEM_MARKER, ignoreCase = true) ||
            text.contains("Examples:", ignoreCase = true) ||
            text.contains("Caretaker:", ignoreCase = true)
    }

    private fun extractReplyAfterPromptEcho(text: String): String {
        val youMarkers = FEW_SHOT_REPLY.findAll(text).toList()
        if (youMarkers.isNotEmpty()) {
            return text.substring(youMarkers.last().range.last + 1).trim()
        }
        val inlineYou = INLINE_FEW_SHOT_REPLY.findAll(text).toList()
        if (inlineYou.isNotEmpty()) {
            val reply = text.substring(inlineYou.last().range.last + 1).trim()
            if (reply.isNotBlank() && !reply.contains(SYSTEM_MARKER, ignoreCase = true)) {
                return reply
            }
        }
        val caretakerMarkers = CARETAKER_TURN.findAll(text).toList()
        if (caretakerMarkers.isNotEmpty()) {
            val afterCaretaker = text.substring(caretakerMarkers.last().range.last + 1).trim()
            if (afterCaretaker.isNotBlank() && !afterCaretaker.contains(SYSTEM_MARKER, ignoreCase = true)) {
                return afterCaretaker.removePrefix("You:").trimStart()
            }
        }
        val userMarkers = USER_TURN.findAll(text).toList()
        if (userMarkers.isNotEmpty()) {
            val afterUser = text.substring(userMarkers.last().range.last + 1).trim()
            if (afterUser.startsWith("You:", ignoreCase = true)) {
                return afterUser.removePrefix("You:").trimStart()
            }
            if (afterUser.isNotBlank() && !afterUser.contains(SYSTEM_MARKER, ignoreCase = true)) {
                return afterUser
            }
        }
        val inlineUser = INLINE_USER_TURN.findAll(text).toList()
        if (inlineUser.isNotEmpty()) {
            val afterUser = text.substring(inlineUser.last().range.last + 1).trim()
            if (afterUser.startsWith("You:", ignoreCase = true)) {
                return afterUser.removePrefix("You:").trimStart()
            }
            if (afterUser.isNotBlank() && !afterUser.contains(SYSTEM_MARKER, ignoreCase = true)) {
                return afterUser
            }
        }
        return text.replace(
            Regex("(?is)^You are a Pixel Friend AI pet.*?(?=\nUser:|\nYou:|User:|You:|$)"),
            "",
        ).trim()
    }

    private fun normalizeRaw(raw: String): String = sanitizeModelEcho(raw)

    private fun normalizedLines(raw: String): List<String> =
        normalizeRaw(raw).lines().map { it.trim() }.filter { it.isNotBlank() }

    internal fun splitDialogueAndExpression(line: String): Pair<String, PetExpression?> {
        val pipe = line.lastIndexOf('|')
        if (pipe <= 0 || pipe >= line.length - 1) {
            return line.trim() to null
        }
        val text = line.substring(0, pipe).trim()
        val tag = line.substring(pipe + 1).trim().uppercase()
        val expression = runCatching { PetExpression.valueOf(tag) }.getOrNull()
        return text to expression
    }

    fun stripForStreaming(partial: String): String {
        val normalized = normalizeRaw(partial)
        val pipe = normalized.indexOf('|')
        val body = if (pipe >= 0) normalized.substring(0, pipe) else normalized
        return normalizedLines(body).joinToString(" ").trim()
    }

    fun fallback(request: PetReactionRequest): PetReaction {
        val line = when {
            request.moodPulse -> PetMoodDescriber.templateLine(request.state)
            request.userMessage != null && PetStatusSnapshot.isStatusQuestion(request.userMessage) ->
                PetNeedRules.statusReply(request.state)
            request.userMessage != null && PetGameFaqMatcher.isGameQuestion(request.userMessage) ->
                PetGameFaqMatcher.bestAnswer(request.userMessage)
                    ?: "Hmm, my on-device brain hiccuped — ask again in a sec?"
            request.userMessage != null ->
                "Hmm, my on-device brain hiccuped — ask again in a sec?"
            request.lastAction != null -> "After ${request.lastAction}, I'm glad you're here!"
            else -> "Beep! I'm your pocket AI friend."
        }
        val maxLen = if (request.userMessage != null) MAX_TALK_LEN else MAX_LEN
        return PetReaction(dialogue = line.take(maxLen))
    }
}
