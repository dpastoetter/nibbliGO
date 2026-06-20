package com.nibbli.nibbligo.core.pet.llm

import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTokenEstimatorTest {
    @Test
    fun trimToTokenBudget_shortensLongText() {
        val long = "word ".repeat(500)
        val trimmed = PromptTokenEstimator.trimToTokenBudget(long, 50)
        assertTrue(trimmed.length <= 50 * 4 + 1)
        assertTrue(trimmed.endsWith("…"))
    }
}
