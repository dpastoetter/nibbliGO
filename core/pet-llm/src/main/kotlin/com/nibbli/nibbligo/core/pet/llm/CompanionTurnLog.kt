package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.model.MessageRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionTurnLog @Inject constructor(
    private val chatRepository: ChatRepository,
    private val petModelResolver: PetModelResolver,
) {
    suspend fun recentTurns(limit: Int = 6, excludeCurrentUserMessage: String? = null): List<TalkTurnPair> {
        val modelId = runCatching { petModelResolver.resolve() }.getOrNull() ?: return emptyList()
        val messages = chatRepository.getRecentMessagesForTitle(
            title = PetTalkChatRecorder.CONVERSATION_TITLE,
            modelId = modelId,
            limit = limit * 2 + 2,
        )
        if (messages.isEmpty()) return emptyList()

        val pairs = mutableListOf<TalkTurnPair>()
        var pendingUser: String? = null
        for (message in messages) {
            when (message.role) {
                MessageRole.USER -> pendingUser = message.content.trim()
                MessageRole.ASSISTANT -> {
                    val user = pendingUser ?: continue
                    pendingUser = null
                    pairs.add(
                        TalkTurnPair(
                            userMessage = user,
                            petDialogue = message.content.trim(),
                        ),
                    )
                }
                else -> Unit
            }
        }

        val exclude = excludeCurrentUserMessage?.trim()?.takeIf { it.isNotBlank() }
        val filtered = if (exclude != null && pairs.lastOrNull()?.userMessage == exclude) {
            pairs.dropLast(1)
        } else {
            pairs
        }
        return filtered.takeLast(limit)
    }

    suspend fun clearThread() {
        val modelId = runCatching { petModelResolver.resolve() }.getOrNull() ?: return
        val conversation = chatRepository.findConversationByTitle(PetTalkChatRecorder.CONVERSATION_TITLE)
            ?: return
        chatRepository.deleteMessagesForConversation(conversation.id)
        chatRepository.updateConversation(
            conversation.copy(
                modelId = modelId,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }
}
