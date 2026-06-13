package com.nibbli.nibbligo.core.pet.llm

data class ParsedTalkTurn(
    val reaction: PetReaction,
    val replySuggestions: List<String>,
)

object PetReplySuggestionParser {
    private val REPLIES_BLOCK = Regex("""(?i)\n---\s*\n\s*REPLIES\s*:\s*(.+)""", RegexOption.DOT_MATCHES_ALL)
    private val INLINE_REPLIES = Regex("""(?i)\nREPLIES\s*:\s*(.+)""", RegexOption.DOT_MATCHES_ALL)
    private val STREAM_SPLIT = Regex("""\n---""")

    fun parseTalkWithSuggestions(
        raw: String,
        petName: String? = null,
        caretakerName: String? = null,
        lastUserMessage: String? = null,
    ): ParsedTalkTurn {
        val sanitized = PetReactionParser.sanitizeModelEcho(raw)
        val (dialogueRaw, repliesRaw) = splitDialogueAndReplies(sanitized)
        val reaction = PetReactionParser.parseTalk(dialogueRaw, petName, caretakerName)
        val suggestions = PetReplySuggestionSanitizer.sanitize(
            parseRepliesBlock(repliesRaw),
            lastUserMessage,
        )
        return ParsedTalkTurn(
            reaction = reaction.copy(replySuggestions = suggestions),
            replySuggestions = suggestions,
        )
    }

    fun parseRepliesOnly(raw: String, lastUserMessage: String? = null): List<String> {
        val trimmed = raw.trim()
        val block = when {
            trimmed.contains("REPLIES:", ignoreCase = true) ->
                trimmed.substringAfter("REPLIES:", "").trim()
            else -> trimmed
        }
        return PetReplySuggestionSanitizer.sanitize(parseRepliesBlock(block), lastUserMessage)
    }

    fun stripSuggestionsForStreaming(
        partial: String,
        petName: String? = null,
        caretakerName: String? = null,
    ): String = PetReactionParser.stripForStreaming(partial, petName, caretakerName)

    internal fun splitDialogueAndReplies(raw: String): Pair<String, String?> {
        val trimmed = raw.trim()
        REPLIES_BLOCK.find(trimmed)?.let { match ->
            return trimmed.substring(0, match.range.first).trim() to match.groupValues[1].trim()
        }
        val splitIndex = trimmed.indexOf("\n---")
        if (splitIndex >= 0) {
            val tail = trimmed.substring(splitIndex + 1).trim()
            val replies = tail.removePrefix("---").trim()
            return trimmed.substring(0, splitIndex).trim() to replies
        }
        INLINE_REPLIES.find(trimmed)?.let { match ->
            return trimmed.substring(0, match.range.first).trim() to match.groupValues[1].trim()
        }
        return trimmed to null
    }

    internal fun stripRepliesSuffix(partial: String): String {
        val split = STREAM_SPLIT.split(partial, limit = 2)
        if (split.size > 1) return split[0].trimEnd()
        val repliesIndex = partial.indexOf("\nREPLIES:", ignoreCase = true)
        if (repliesIndex >= 0) return partial.substring(0, repliesIndex).trimEnd()
        return partial
    }

    private fun parseRepliesBlock(repliesRaw: String?): List<String> {
        if (repliesRaw.isNullOrBlank()) return emptyList()
        val body = repliesRaw.trim()
            .removePrefix("REPLIES:")
            .removePrefix("replies:")
            .trim()
        if (body.isBlank()) return emptyList()
        return body.split('|').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
