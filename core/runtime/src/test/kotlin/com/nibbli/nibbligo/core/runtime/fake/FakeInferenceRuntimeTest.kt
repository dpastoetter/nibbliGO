package com.nibbli.nibbligo.core.runtime.fake

import com.nibbli.nibbligo.core.model.ChatInferenceRequest
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.RuntimeResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeInferenceRuntimeTest {

    private lateinit var runtime: FakeInferenceRuntime

    @Before
    fun setup() {
        runtime = FakeInferenceRuntime()
    }

    @Test
    fun streamChat_emits_tokens_after_load() = runTest {
        runtime.ensureModelLoaded("nibbli-fast")
        val chunks = runtime.streamChat(
            ChatInferenceRequest(
                modelId = "nibbli-fast",
                messages = listOf(
                    ChatMessage(
                        conversationId = 1,
                        role = MessageRole.USER,
                        content = "hello",
                        timestampMillis = 0,
                        modelId = "nibbli-fast",
                    ),
                ),
                params = GenerationParams(),
            ),
        ).toList()
        assertTrue(chunks.any { it.token.isNotEmpty() })
        assertTrue(chunks.last().isComplete)
    }

    @Test
    fun vision_unsupported_for_text_only_model() = runTest {
        runtime.ensureModelLoaded("nibbli-fast")
        val result = runtime.analyzeImage(
            com.nibbli.nibbligo.core.model.VisionRequest(
                "nibbli-fast",
                "file://test",
                "What is this?",
            ),
        )
        assertTrue(result is RuntimeResult.Unsupported)
    }
}
