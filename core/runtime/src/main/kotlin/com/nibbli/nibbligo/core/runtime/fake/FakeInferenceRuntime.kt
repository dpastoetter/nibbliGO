package com.nibbli.nibbligo.core.runtime.fake

import com.nibbli.nibbligo.core.model.AgentMessageRole
import com.nibbli.nibbligo.core.model.AgentRequest
import com.nibbli.nibbligo.core.model.AgentTurn
import com.nibbli.nibbligo.core.model.BenchmarkMetrics
import com.nibbli.nibbligo.core.model.ChatInferenceRequest
import com.nibbli.nibbligo.core.model.CompletionRequest
import com.nibbli.nibbligo.core.model.InferenceChunk
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.Modality
import com.nibbli.nibbligo.core.model.ModelCapabilities
import com.nibbli.nibbligo.core.model.PromptPreset
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.TranscriptionRequest
import com.nibbli.nibbligo.core.model.VisionRequest
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import com.nibbli.nibbligo.core.model.ModelCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FakeInferenceRuntime @Inject constructor() : InferenceRuntime {

    override val runtimeKind: RuntimeKind = RuntimeKind.FAKE

    private val loadedModels = ConcurrentHashMap.newKeySet<String>()

    override suspend fun ensureModelLoaded(modelId: String): RuntimeResult<Unit> {
        if (ModelCatalog.find(modelId) == null) {
            return RuntimeResult.Error("Model not found: $modelId")
        }
        delay(Random.nextLong(300, 800))
        loadedModels.add(modelId)
        return RuntimeResult.Success(Unit)
    }

    override fun unloadModel(modelId: String) {
        loadedModels.remove(modelId)
    }

    override fun streamChat(request: ChatInferenceRequest): Flow<InferenceChunk> = flow {
        if (!loadedModels.contains(request.modelId)) {
            emit(InferenceChunk("[Load a model first]", isComplete = true))
            return@flow
        }
        val lastUser = request.messages.lastOrNull { it.role == MessageRole.USER }?.content
          ?: "Hello"
        val response = buildChatResponse(lastUser, request.params.systemPrompt)
        val words = response.split(" ")
        val tokenDelay = (20 / request.params.temperature.coerceIn(0.1f, 2f)).toLong().coerceIn(15, 80)
        delay(120)
        words.forEachIndexed { index, word ->
            val token = if (index == 0) word else " $word"
            emit(InferenceChunk(token))
            delay(tokenDelay)
        }
        emit(InferenceChunk("", isComplete = true))
    }

    override suspend fun complete(request: CompletionRequest): RuntimeResult<String> {
        if (!loadedModels.contains(request.modelId)) {
            return RuntimeResult.Error("Model not loaded")
        }
        delay(200)
        val preset = request.preset
        val output = when (preset) {
            PromptPreset.SUMMARIZE -> "Summary: ${request.prompt.take(120)}…"
            PromptPreset.REWRITE -> "Rewritten: ${request.prompt} (polished for clarity)"
            PromptPreset.CLASSIFY -> "Category: general · confidence: 0.87"
            PromptPreset.EXTRACT -> "Extracted: key facts from input."
            PromptPreset.BRAINSTORM -> "Ideas: 1) Explore locally 2) Try Prompt Lab 3) Train nibbli"
            PromptPreset.TRANSLATE -> "Translation [demo]: ${request.prompt}"
            PromptPreset.PLAN -> "Plan: Step 1 — define goal. Step 2 — run on-device. Step 3 — review."
            null -> "Output: ${request.prompt.take(200)}"
        }
        return RuntimeResult.Success(output)
    }

    override suspend fun analyzeImage(request: VisionRequest): RuntimeResult<String> {
        val caps = capabilitiesFor(request.modelId)
        if (!caps.supportsVision) return RuntimeResult.Unsupported
        if (!loadedModels.contains(request.modelId)) {
            return RuntimeResult.Error("Model not loaded")
        }
        delay(400)
        return RuntimeResult.Success(
            "On-device vision (demo): I see shapes and colors. " +
                "Question: \"${request.question}\" — " +
                "This looks like a everyday scene worth describing offline.",
        )
    }

    override suspend fun transcribeAudio(request: TranscriptionRequest): RuntimeResult<String> {
        val caps = capabilitiesFor(request.modelId)
        if (!caps.supportsAudio) return RuntimeResult.Unsupported
        if (!loadedModels.contains(request.modelId)) {
            return RuntimeResult.Error("Model not loaded")
        }
        delay(500)
        val transcript = "[Demo transcript] Local audio captured and processed on-device."
        return if (request.summarize) {
            RuntimeResult.Success("$transcript\n\nSummary: Short spoken notes about your day.")
        } else {
            RuntimeResult.Success(transcript)
        }
    }

    override suspend fun runBenchmark(modelId: String): RuntimeResult<BenchmarkMetrics> {
        val info = ModelCatalog.find(modelId)
          ?: return RuntimeResult.Error("Unknown model")
        if (!loadedModels.contains(modelId)) {
            val load = ensureModelLoaded(modelId)
            if (load !is RuntimeResult.Success) return load as RuntimeResult<BenchmarkMetrics>
        }
        delay(600)
        return RuntimeResult.Success(
            BenchmarkMetrics(
                timeToFirstTokenMs = Random.nextLong(80, 220),
                tokensPerSecond = Random.nextDouble(18.0, 42.0).toFloat(),
                estimatedMemoryMb = info.estimatedRamMb,
                thermalNote = when {
                    info.estimatedRamMb >= 1024 -> "May warm device during long runs"
                    else -> "Efficient for typical sessions"
                },
            ),
        )
    }

    override fun capabilitiesFor(modelId: String): ModelCapabilities {
        val info = ModelCatalog.find(modelId)
        val supportsTools = modelId == "nibbli-fast" ||
            modelId == "gemma-4-e2b-it" ||
            modelId == "functiongemma-270m"
        return ModelCapabilities(
            modelId = modelId,
            supportsChat = info != null,
            supportsVision = info?.modalities?.contains(Modality.VISION) == true,
            supportsAudio = info?.modalities?.contains(Modality.AUDIO) == true,
            supportsStreaming = info != null,
            supportsToolCalling = supportsTools,
            supportsThinkingTrace = modelId == "gemma-4-e2b-it",
        )
    }

    override suspend fun generateWithTools(request: AgentRequest): RuntimeResult<AgentTurn> {
        if (!loadedModels.contains(request.modelId)) {
            return RuntimeResult.Error("Model not loaded")
        }
        delay(150)
        if (request.toolResults.isNotEmpty()) {
            val lastResult = request.toolResults.last().outputJson
            return RuntimeResult.Success(
                AgentTurn.FinalText(
                    text = "Done on-device. Result: $lastResult",
                    thinkingTrace = listOf("Reviewed tool output locally."),
                ),
            )
        }
        val lastUser = request.messages.lastOrNull { it.role == AgentMessageRole.USER }?.content.orEmpty()
        val thinking = when (request.modelId) {
            "gemma-4-e2b-it" -> listOf(
                "Planning next step on-device…",
                "Checking available tools locally.",
            )
            "functiongemma-270m" -> listOf("Routing mobile action on-device…")
            else -> listOf("Planning next step on-device…")
        }
        if (request.modelId == "functiongemma-270m") {
            return mobileActionTurn(lastUser, thinking)
        }
        return when {
            lastUser.contains("remind", ignoreCase = true) -> {
                RuntimeResult.Success(
                    AgentTurn.ToolCalls(
                        calls = listOf(
                            com.nibbli.nibbligo.core.model.ToolCall(
                                toolId = "reminder_create",
                                argumentsJson = """{"title":"${lastUser.take(40)}","notes":"from agent"}""",
                            ),
                        ),
                        thinkingTrace = thinking,
                    ),
                )
            }
            lastUser.contains("note", ignoreCase = true) -> {
                RuntimeResult.Success(
                    AgentTurn.ToolCalls(
                        calls = listOf(
                            com.nibbli.nibbligo.core.model.ToolCall(
                                toolId = "notes_save",
                                argumentsJson = """{"title":"Agent note","body":${org.json.JSONObject.quote(lastUser)}}""",
                            ),
                        ),
                        thinkingTrace = thinking,
                    ),
                )
            }
            lastUser.contains("clipboard", ignoreCase = true) ||
                lastUser.contains("summarize", ignoreCase = true) -> {
                RuntimeResult.Success(
                    AgentTurn.ToolCalls(
                        calls = listOf(
                            com.nibbli.nibbligo.core.model.ToolCall(
                                toolId = "clipboard_summarize",
                                argumentsJson = "{}",
                            ),
                        ),
                        thinkingTrace = thinking,
                    ),
                )
            }
            else -> RuntimeResult.Success(
                AgentTurn.FinalText(
                    text = buildChatResponse(lastUser, request.params.systemPrompt),
                    thinkingTrace = thinking,
                ),
            )
        }
    }

    private fun mobileActionTurn(
        lastUser: String,
        thinking: List<String>,
    ): RuntimeResult<AgentTurn> = when {
        lastUser.contains("flashlight", ignoreCase = true) ||
            lastUser.contains("torch", ignoreCase = true) -> {
            RuntimeResult.Success(
                AgentTurn.ToolCalls(
                    calls = listOf(
                        com.nibbli.nibbligo.core.model.ToolCall(
                            toolId = "flashlight_toggle",
                            argumentsJson = """{"on":true}""",
                        ),
                    ),
                    thinkingTrace = thinking,
                ),
            )
        }
        lastUser.contains("settings", ignoreCase = true) -> {
            RuntimeResult.Success(
                AgentTurn.ToolCalls(
                    calls = listOf(
                        com.nibbli.nibbligo.core.model.ToolCall(
                            toolId = "open_settings",
                            argumentsJson = """{"screen":"main"}""",
                        ),
                    ),
                    thinkingTrace = thinking,
                ),
            )
        }
        lastUser.contains("clipboard", ignoreCase = true) -> {
            RuntimeResult.Success(
                AgentTurn.ToolCalls(
                    calls = listOf(
                        com.nibbli.nibbligo.core.model.ToolCall(
                            toolId = "read_clipboard",
                            argumentsJson = "{}",
                        ),
                    ),
                    thinkingTrace = thinking,
                ),
            )
        }
        else -> RuntimeResult.Success(
            AgentTurn.FinalText(
                text = "FunctionGemma (demo): try “toggle flashlight”, “open settings”, or “read clipboard”.",
                thinkingTrace = thinking,
            ),
        )
    }

    private fun buildChatResponse(userMessage: String, systemPrompt: String): String {
        return when {
            userMessage.contains("hello", ignoreCase = true) ->
                "Hi! I'm nibbli running entirely on your device. How can I help?"
            userMessage.contains("model", ignoreCase = true) ->
                "You're using the fake runtime — swap in LiteRT when ready. No cloud calls here."
            else ->
                "On-device reply: I heard \"$userMessage\". " +
                    "(${systemPrompt.take(40)}…)"
        }
    }
}
