package com.nibbli.nibbligo.core.model

enum class RuntimeKind {
    LITERT,
}

sealed class RuntimeResult<out T> {
    data class Success<T>(val data: T) : RuntimeResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : RuntimeResult<Nothing>()
    data object Unsupported : RuntimeResult<Nothing>()
    data object LowMemory : RuntimeResult<Nothing>()
}

data class InferenceChunk(
    val token: String,
    val isComplete: Boolean = false,
)

data class ChatInferenceRequest(
    val modelId: String,
    val messages: List<ChatMessage>,
    val params: GenerationParams,
)

data class CompletionRequest(
    val modelId: String,
    val prompt: String,
    val preset: PromptPreset? = null,
    val params: GenerationParams = GenerationParams(),
)

data class VisionRequest(
    val modelId: String,
    val imageUri: String,
    val question: String,
)

data class TranscriptionRequest(
    val modelId: String,
    val audioUri: String,
    val summarize: Boolean = false,
)

data class BenchmarkMetrics(
    val timeToFirstTokenMs: Long,
    val tokensPerSecond: Float,
    val estimatedMemoryMb: Int,
    val thermalNote: String,
)

data class BenchmarkRun(
    val id: Long = 0,
    val modelId: String,
    val metrics: BenchmarkMetrics,
    val timestampMillis: Long,
)
