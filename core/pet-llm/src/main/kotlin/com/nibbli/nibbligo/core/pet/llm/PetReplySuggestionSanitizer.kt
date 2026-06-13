package com.nibbli.nibbligo.core.pet.llm

object PetReplySuggestionSanitizer {
    const val MAX_SUGGESTIONS = 4
    const val MAX_LABEL_LENGTH = 28

    private val ROLE_PREFIX = Regex("""(?i)^(you|user|caretaker|assistant|system)\s*:\s*""")

    fun sanitize(rawOptions: List<String>, lastUserMessage: String? = null): List<String> {
        val lastUser = lastUserMessage?.trim()?.lowercase()
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (option in rawOptions) {
            val cleaned = cleanLabel(option)
            if (cleaned.isBlank()) continue
            val key = cleaned.lowercase()
            if (key in seen) continue
            if (lastUser != null && key == lastUser) continue
            seen += key
            result += cleaned.take(MAX_LABEL_LENGTH).trim()
            if (result.size >= MAX_SUGGESTIONS) break
        }
        return result
    }

    fun cleanLabel(raw: String): String {
        var text = raw.trim()
            .replace('\n', ' ')
            .replace('|', ' ')
            .replace(Regex("""[*_`#]"""), "")
            .trim()
        text = ROLE_PREFIX.replace(text, "").trim()
        return text
    }
}
