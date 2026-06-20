package com.nibbli.nibbligo.core.pet.llm

/** Rough token estimate for prompt budgeting (chars / 4). */
object PromptTokenEstimator {
    fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    fun trimToTokenBudget(text: String, maxTokens: Int): String {
        val maxChars = maxTokens * 4
        if (text.length <= maxChars) return text
        return text.take(maxChars).trimEnd() + "…"
    }
}
