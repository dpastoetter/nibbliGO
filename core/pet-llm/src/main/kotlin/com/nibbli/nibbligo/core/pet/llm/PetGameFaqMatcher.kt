package com.nibbli.nibbligo.core.pet.llm

enum class FaqPromptProfile {
    COMPACT,
    STANDARD,
    RICH,
}

object PetGameFaqMatcher {
    private const val COMPACT_MAX_CHARS = 200
    private const val STANDARD_MAX_CHARS = 450

    private val META_PHRASES = listOf(
        "how do i",
        "how do you",
        "how to",
        "how does",
        "how can i",
        "what is",
        "what are",
        "what does",
        "what do i",
        "tell me about",
        "explain",
        "help me",
        "game work",
        "rules",
        "unlock",
        "evolve",
        "evolution",
        "minigame",
        "looks",
        "cosmetic",
        "notification",
        "download",
        "install",
        "take care",
        "caretaker",
    )

    private val TOPIC_WORDS = setOf(
        "hunger", "hungry", "feed", "meal", "snack", "food",
        "hygiene", "clean", "mess", "dirty",
        "energy", "sleep", "tired", "nap",
        "sick", "medicine", "meds", "health",
        "trust", "skill", "train", "evolve", "evolution", "hatch", "egg",
        "die", "death", "dead", "rebirth",
        "looks", "cosmetic", "collar", "patch", "aura",
        "minigame", "catch", "treat", "game",
        "notification", "model", "llm", "talk", "assist", "agent",
        "care", "play", "lcd", "personality", "mood pulse",
    )

    fun isGameQuestion(message: String): Boolean {
        if (PetStatusSnapshot.isStatusQuestion(message)) return false
        val lower = message.lowercase().trim()
        if (lower.isBlank()) return false
        if (META_PHRASES.any { lower.contains(it) }) return true
        val words = lower.split(Regex("\\W+")).filter { it.length > 2 }
        return words.any { it in TOPIC_WORDS }
    }

    fun select(
        message: String,
        maxEntries: Int,
        maxChars: Int,
    ): List<PetGameFaqEntry> {
        val lower = message.lowercase()
        val scored = PetGameFaq.entries
            .map { entry -> entry to scoreEntry(entry, lower) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }

        val picked = mutableListOf<PetGameFaqEntry>()
        var usedChars = 0
        for ((entry, _) in scored) {
            if (picked.size >= maxEntries) break
            val nextLen = entry.answer.length + if (picked.isEmpty()) 0 else 1
            if (usedChars + nextLen > maxChars && picked.isNotEmpty()) break
            picked.add(entry)
            usedChars += nextLen
        }

        if (picked.isEmpty()) {
            return listOf(PetGameFaq.findById("care_basics")!!).take(maxEntries)
        }
        return picked
    }

    fun formatForPrompt(message: String, profile: FaqPromptProfile): String {
        val (maxEntries, maxChars) = when (profile) {
            FaqPromptProfile.COMPACT -> 1 to COMPACT_MAX_CHARS
            FaqPromptProfile.STANDARD -> 2 to STANDARD_MAX_CHARS
            FaqPromptProfile.RICH -> 2 to STANDARD_MAX_CHARS
        }
        return select(message, maxEntries, maxChars)
            .joinToString("\n") { "- ${it.answer}" }
    }

    fun bestAnswer(message: String): String? {
        val entry = select(message, maxEntries = 1, maxChars = Int.MAX_VALUE).firstOrNull()
        return entry?.answer
    }

    /** Skip LLM when FAQ match is strong enough for an instant in-character reply. */
    fun confidentInstantAnswer(message: String): String? {
        if (!isGameQuestion(message)) return null
        val lower = message.lowercase().trim()
        val scored = PetGameFaq.entries
            .map { entry -> entry to scoreEntry(entry, lower) }
            .filter { (_, score) -> score > 0 }
        val top = scored.maxByOrNull { it.second } ?: return null
        if (top.second < 3) return null
        return top.first.answer
    }

    private fun scoreEntry(entry: PetGameFaqEntry, lowerMessage: String): Int {
        var score = 0
        entry.keywords.forEach { keyword ->
            if (lowerMessage.contains(keyword)) score += keyword.count { it == ' ' } + 2
        }
        entry.topics.forEach { topic ->
            if (lowerMessage.contains(topic)) score += 1
        }
        return score
    }
}
