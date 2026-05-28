package com.nibbli.nibbligo.core.model

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL,
}

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val notes: String? = null,
    val timestampMillis: Long,
    val modelId: String,
)

data class Conversation(
    val id: Long = 0,
    val title: String,
    val modelId: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class GenerationParams(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    val systemPrompt: String = "You are nibbli, a helpful on-device companion.",
)
