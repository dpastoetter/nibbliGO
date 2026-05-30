package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.MessageRole
import javax.inject.Inject
import javax.inject.Singleton

/** Mirrors Home Pixel Friend talk into Local Chat history. */
@Singleton
class PetTalkChatRecorder @Inject constructor(
    private val chatRepository: ChatRepository,
    private val petModelResolver: PetModelResolver,
) {
    suspend fun recordUserMessage(text: String, modelId: String, timestampMillis: Long = System.currentTimeMillis()) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val conversationId = chatRepository.getOrCreateConversation(CONVERSATION_TITLE, modelId)
        chatRepository.saveMessage(
            ChatMessage(
                conversationId = conversationId,
                role = MessageRole.USER,
                content = trimmed,
                timestampMillis = timestampMillis,
                modelId = modelId,
            ),
        )
        touchConversation(conversationId, modelId)
    }

    suspend fun recordAssistantMessage(
        text: String,
        modelId: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val conversationId = chatRepository.getOrCreateConversation(CONVERSATION_TITLE, modelId)
        chatRepository.saveMessage(
            ChatMessage(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = trimmed,
                timestampMillis = timestampMillis,
                modelId = modelId,
            ),
        )
        touchConversation(conversationId, modelId)
    }

    suspend fun resolvePetModelId(): String = petModelResolver.resolve()

    private suspend fun touchConversation(conversationId: Long, modelId: String) {
        val existing = chatRepository.findConversationByTitle(CONVERSATION_TITLE) ?: return
        chatRepository.updateConversation(
            existing.copy(
                id = conversationId,
                modelId = modelId,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    companion object {
        const val CONVERSATION_TITLE = "Pixel Friend (Home)"
    }
}
