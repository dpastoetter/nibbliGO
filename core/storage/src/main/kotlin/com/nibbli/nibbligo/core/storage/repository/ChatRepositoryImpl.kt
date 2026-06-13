package com.nibbli.nibbligo.core.storage.repository

import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.Conversation
import com.nibbli.nibbligo.core.storage.local.dao.ConversationDao
import com.nibbli.nibbligo.core.storage.local.dao.MessageDao
import com.nibbli.nibbligo.core.storage.mapper.toDomain
import com.nibbli.nibbligo.core.storage.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
  private val conversationDao: ConversationDao,
  private val messageDao: MessageDao,
) : ChatRepository {

  override fun observeConversations(): Flow<List<Conversation>> =
    conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

  override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
    messageDao.observe(conversationId).map { list -> list.map { it.toDomain() } }

  override suspend fun createConversation(modelId: String, title: String): Long {
    val now = System.currentTimeMillis()
    return conversationDao.insert(
      com.nibbli.nibbligo.core.storage.local.entity.ConversationEntity(
        title = title,
        modelId = modelId,
        createdAtMillis = now,
        updatedAtMillis = now,
      ),
    )
  }

  override suspend fun findConversationByTitle(title: String): Conversation? =
    conversationDao.findByTitle(title)?.toDomain()

  override suspend fun getOrCreateConversation(title: String, modelId: String): Long {
    val existing = conversationDao.findByTitle(title)
    if (existing != null) {
      if (existing.modelId != modelId) {
        conversationDao.update(
          existing.copy(modelId = modelId, updatedAtMillis = System.currentTimeMillis()),
        )
      }
      return existing.id
    }
    return createConversation(modelId, title)
  }

  override suspend fun getRecentMessagesForTitle(
    title: String,
    modelId: String,
    limit: Int,
  ): List<ChatMessage> {
    val conversation = conversationDao.findByTitle(title) ?: return emptyList()
    if (limit <= 0) return emptyList()
    return messageDao.getRecent(conversation.id, limit)
      .asReversed()
      .map { it.toDomain() }
  }

  override suspend fun saveMessage(message: ChatMessage) {
    messageDao.insert(message.toEntity())
  }

  override suspend fun deleteMessagesForConversation(conversationId: Long) {
    messageDao.deleteForConversation(conversationId)
  }

  override suspend fun updateConversation(conversation: Conversation) {
    conversationDao.update(conversation.toEntity())
  }

  override suspend fun deleteAllConversations() {
    conversationDao.deleteAll()
  }
}
