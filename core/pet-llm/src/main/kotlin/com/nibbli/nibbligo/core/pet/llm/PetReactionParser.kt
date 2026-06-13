package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetNeedRules

object PetReactionParser {
    private const val MAX_LEN = 180
    private const val DEFAULT_PET_NAME = "nibbli"
    private val LEADING_ROLE_WORD = Regex("""(?i)^(?:system|user|assistant|model)\s*""")
    private val SYSTEM_ROLE_GLUE = Regex("""(?i)^system(?=You are)""")
    private val FEW_SHOT_REPLY = Regex("""(?m)^You:\s""")
    private val USER_TURN = Regex("""(?m)^User:\s""")
    private val CARETAKER_TURN = Regex("""(?m)^Caretaker:\s""")
    private val INLINE_FEW_SHOT_REPLY = Regex("""You:\s""")
    private val INLINE_USER_TURN = Regex("""User:\s""")
    private const val SYSTEM_MARKER = "You are a Pixel Friend AI pet"

    fun parse(
        raw: String,
        petName: String? = null,
        caretakerName: String? = null,
    ): PetReaction {
        val primary = normalizedLines(raw, petName, caretakerName).firstOrNull()
            ?: normalizeRaw(raw, petName, caretakerName)
        return reactionFromDialogue(primary, MAX_LEN, petName, caretakerName)
    }

    /** User talk replies — keep all streamed lines, aligned with [stripForStreaming]. */
    fun parseTalk(
        raw: String,
        petName: String? = null,
        caretakerName: String? = null,
    ): PetReaction {
        val combined = collapseCommaRepetition(
            normalizedLines(raw, petName, caretakerName).joinToString(" ").ifBlank {
                normalizeRaw(raw, petName, caretakerName)
            },
        )
        return reactionFromDialogue(combined, PetTalkLimits.RUNAWAY_MAX_CHARS, petName, caretakerName)
    }

    /** True when the model loops the same comma-separated phrase (common status-echo failure). */
    fun hasDegenerateRepetition(dialogue: String): Boolean {
        val segments = dialogue.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.size < 3) return false
        val normalized = segments.map { it.lowercase() }
        val dominantCount = normalized.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
        if (dominantCount >= 3 && dominantCount.toFloat() / segments.size >= 0.6f) return true
        val suffix = normalized.firstOrNull()?.removePrefix("i'm ")?.trim().orEmpty()
        if (suffix.length >= 8) {
            val suffixRepeats = normalized.drop(1).count { it == suffix || it.endsWith(suffix) }
            if (suffixRepeats >= 2 && suffixRepeats + 1 >= segments.size * 0.6f) return true
        }
        return false
    }

    fun reconcileTalkStream(
        parsed: PetReaction,
        streamedText: String,
        petName: String? = null,
        caretakerName: String? = null,
    ): PetReaction {
        val streamedReaction = parseTalk(streamedText, petName, caretakerName)
        if (streamedReaction.dialogue.isBlank()) return parsed
        val mergedExpression = parsed.suggestedExpression ?: streamedReaction.suggestedExpression
        return if (streamedReaction.dialogue.length > parsed.dialogue.length) {
            parsed.copy(
                dialogue = streamedReaction.dialogue,
                suggestedExpression = mergedExpression,
            )
        } else {
            parsed.copy(suggestedExpression = mergedExpression)
        }
    }

    private fun reactionFromDialogue(
        line: String,
        maxLen: Int,
        petName: String? = null,
        caretakerName: String? = null,
    ): PetReaction {
        val (dialogue, expression) = splitDialogueAndExpression(line)
        val cleanDialogue = normalizeFirstPersonDialogue(dialogue, petName, caretakerName)
            .take(maxLen)
            .ifBlank { "…" }
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

    private fun normalizeRaw(
        raw: String,
        petName: String? = null,
        caretakerName: String? = null,
    ): String = normalizeFirstPersonDialogue(sanitizeModelEcho(raw), petName, caretakerName)

    private fun normalizedLines(
        raw: String,
        petName: String? = null,
        caretakerName: String? = null,
    ): List<String> = normalizeRaw(raw, petName, caretakerName).lines().map { it.trim() }.filter { it.isNotBlank() }

    /** Strip third-person self-reference ("Nibbli, …" / "Pixel is …") at reply start. */
    internal fun normalizeFirstPersonDialogue(
        dialogue: String,
        petName: String? = null,
        caretakerName: String? = null,
    ): String {
        var text = dialogue.trim()
        if (text.isBlank()) return text
        val petNames = buildSet {
            add(DEFAULT_PET_NAME)
            petName?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val caretaker = caretakerName?.trim()?.takeIf { it.isNotBlank() }
        for (name in petNames) {
            val escaped = Regex.escape(name)
            text = Regex("(?i)^$escaped\\s+is\\s+").replaceFirst(text, "I'm ")
            text = Regex("(?i)^$escaped\\s+was\\s+").replaceFirst(text, "I was ")
            text = Regex("(?i)^$escaped\\s+feels?\\s+").replaceFirst(text, "I feel ")
            text = Regex("(?i)^$escaped\\s+loves\\s+").replaceFirst(text, "I love ")
            text = Regex("(?i)^$escaped\\s+needs\\s+").replaceFirst(text, "I need ")
            text = Regex("(?i)^$escaped\\s+wants\\s+").replaceFirst(text, "I want ")
            val addressesCaretaker = caretaker?.equals(name, ignoreCase = true) == true
            if (!addressesCaretaker) {
                text = Regex("(?i)^$escaped[,:\\-]+\\s*").replaceFirst(text, "")
            }
        }
        return text.trim()
    }

    private val EXPRESSION_PATTERN =
        PetExpression.entries.joinToString("|") { Regex.escape(it.name) }

    private val PUNCT_THEN_EXPRESSION = Regex(
        """(?i)[!?.]\s+[\[(]?($EXPRESSION_PATTERN)[\])]?[.!?]*$""",
    )

    private val TRAILING_WORD_EXPRESSION = Regex(
        """(?i)\s+($EXPRESSION_PATTERN)[.!?]*$""",
    )

    internal fun splitDialogueAndExpression(line: String): Pair<String, PetExpression?> {
        val trimmed = line.trim()
        val pipe = trimmed.lastIndexOf('|')
        if (pipe > 0 && pipe < trimmed.length - 1) {
            val text = trimmed.substring(0, pipe).trim()
            val tag = trimmed.substring(pipe + 1).trim().uppercase().trimEnd('.', '!', '?')
            parseExpressionTag(tag)?.let { return text to it }
            if (tag.isNotEmpty() && tag.all { it.isLetter() }) return text to null
        }
        return stripTrailingExpressionToken(trimmed)
    }

    private fun parseExpressionTag(tag: String): PetExpression? =
        runCatching { PetExpression.valueOf(tag.uppercase()) }.getOrNull()

    private fun stripTrailingExpressionToken(text: String): Pair<String, PetExpression?> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size >= 2) {
            val last = lines.last()
                .trimEnd('.', '!', '?')
                .removeSurrounding("(", ")")
                .removeSurrounding("[", "]")
            parseExpressionTag(last)?.let { expr ->
                return lines.dropLast(1).joinToString(" ") to expr
            }
        }

        PUNCT_THEN_EXPRESSION.find(text)?.let { match ->
            parseExpressionTag(match.groupValues[1])?.let { expr ->
                val dialogue = text.substring(0, match.range.first + 1).trimEnd()
                return dialogue to expr
            }
        }

        TRAILING_WORD_EXPRESSION.find(text)?.let { match ->
            val tag = match.groupValues[1]
            if (tag == tag.uppercase() && tag.length >= 4 && match.range.first >= 4) {
                parseExpressionTag(tag)?.let { expr ->
                    val dialogue = text.substring(0, match.range.first).trimEnd(',', ' ')
                    return dialogue to expr
                }
            }
        }

        return text.trim() to null
    }

    fun stripForStreaming(
        partial: String,
        petName: String? = null,
        caretakerName: String? = null,
    ): String {
        val withoutSuggestions = PetReplySuggestionParser.stripRepliesSuffix(partial)
        return stripDialogueForStreaming(withoutSuggestions, petName, caretakerName)
    }

    private fun stripDialogueForStreaming(
        partial: String,
        petName: String? = null,
        caretakerName: String? = null,
    ): String {
        val normalized = normalizeRaw(partial, petName, caretakerName)
        val pipe = normalized.indexOf('|')
        val body = if (pipe >= 0) normalized.substring(0, pipe) else normalized
        val joined = collapseCommaRepetition(
            normalizedLines(body, petName, caretakerName).joinToString(" ").trim(),
        )
        return splitDialogueAndExpression(joined).first
    }

    internal fun collapseCommaRepetition(text: String): String {
        val segments = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.size < 2) return text
        val normalized = segments.map { it.lowercase() }
        val dominant = normalized.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        if (dominant != null) {
            val dominantCount = normalized.count { it == dominant }
            if (dominantCount >= 3 && dominantCount.toFloat() / segments.size >= 0.6f) {
                return segments.first()
            }
            val suffixRepeats = normalized.drop(1).count { it == dominant || it.endsWith(dominant) }
            if (suffixRepeats >= 2 && suffixRepeats + 1 >= segments.size * 0.6f) {
                return segments.first()
            }
        }
        val collapsed = buildList {
            var previous: String? = null
            for (segment in segments) {
                if (segment.equals(previous, ignoreCase = true)) continue
                add(segment)
                previous = segment
            }
        }
        return if (collapsed.size == 1) collapsed.first() else collapsed.joinToString(", ")
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
        val maxLen = if (request.userMessage != null) PetTalkLimits.RUNAWAY_MAX_CHARS else MAX_LEN
        return PetReaction(dialogue = line.take(maxLen))
    }
}
