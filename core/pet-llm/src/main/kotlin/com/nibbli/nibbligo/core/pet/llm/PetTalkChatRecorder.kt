package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.MessageRole
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Mirrors Home Pixel Friend talk into Local Chat history (user + assistant only). */
@Singleton
class PetTalkChatRecorder @Inject constructor(
    private val chatRepository: ChatRepository,
    private val petModelResolver: PetModelResolver,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend fun recordHomeTalkTurn(
        request: PetReactionRequest,
        assistantText: String,
        modelId: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        val userMessage = request.userMessage?.trim().orEmpty()
        val assistant = assistantText.trim()
        if (userMessage.isEmpty() || assistant.isEmpty()) return
        val conversationId = chatRepository.getOrCreateConversation(CONVERSATION_TITLE, modelId)
        recordSimpleTurn(
            conversationId = conversationId,
            modelId = modelId,
            userText = userMessage,
            assistant = assistant,
            timestampMillis = timestampMillis,
        )
        touchConversation(conversationId, modelId, CONVERSATION_TITLE)
    }

    suspend fun recordSimpleTurn(
        conversationId: Long,
        modelId: String,
        userText: String,
        assistant: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        val user = userText.trim()
        val reply = assistant.trim()
        if (user.isEmpty() || reply.isEmpty()) return
        chatRepository.saveMessage(
            ChatMessage(
                conversationId = conversationId,
                role = MessageRole.USER,
                content = user,
                timestampMillis = timestampMillis - 1,
                modelId = modelId,
            ),
        )
        chatRepository.saveMessage(
            ChatMessage(
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = reply,
                timestampMillis = timestampMillis,
                modelId = modelId,
            ),
        )
    }

    /** Pure LLM turns for Prompt Lab — includes system prompt in history for debugging. */
    suspend fun recordPureLlmTurn(
        conversationId: Long,
        modelId: String,
        systemPrompt: String,
        userText: String,
        assistant: String,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        val user = userText.trim()
        val reply = assistant.trim()
        if (user.isEmpty() || reply.isEmpty()) return
        if (systemPrompt.isNotBlank()) {
            val existing = chatRepository.observeMessages(conversationId).first()
            val lastSystem = existing.lastOrNull { it.role == MessageRole.SYSTEM }?.content
            if (lastSystem != systemPrompt.trim()) {
                chatRepository.saveMessage(
                    ChatMessage(
                        conversationId = conversationId,
                        role = MessageRole.SYSTEM,
                        content = systemPrompt.trim(),
                        timestampMillis = timestampMillis - 2,
                        modelId = modelId,
                    ),
                )
            }
        }
        recordSimpleTurn(conversationId, modelId, user, reply, timestampMillis)
    }

    suspend fun resolvePetModelId(): String = petModelResolver.resolve()

    private suspend fun touchConversation(conversationId: Long, modelId: String, title: String) {
        val existing = chatRepository.findConversationByTitle(title) ?: return
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
