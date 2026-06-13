package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.ChatPromptMode
import com.nibbli.nibbligo.core.model.Conversation
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetTalkChatRecorderTest {

    @Test
    fun recordHomeTalkTurn_persistsUserAndAssistantOnly() {
        val chatRepo = FakeChatRepository()
        val recorder = createRecorder(chatRepo)

        kotlinx.coroutines.test.runTest {
            recorder.recordHomeTalkTurn(
                request = PetReactionRequest(
                    state = PetState(name = "nibbli"),
                    userMessage = "How do I evolve?",
                ),
                assistantText = "Grow with care and time!",
                modelId = "smollm2-360m-instruct",
                timestampMillis = 200L,
            )

            assertEquals(1, chatRepo.conversations.size)
            assertEquals(PetTalkChatRecorder.CONVERSATION_TITLE, chatRepo.conversations.first().title)
            assertEquals(2, chatRepo.messages.size)
            assertEquals(MessageRole.USER, chatRepo.messages[0].role)
            assertEquals("How do I evolve?", chatRepo.messages[0].content)
            assertEquals(MessageRole.ASSISTANT, chatRepo.messages[1].role)
            assertTrue(chatRepo.messages[1].content.contains("care"))
        }
    }

    private fun createRecorder(chatRepo: FakeChatRepository): PetTalkChatRecorder {
        val runtime = FakeInferenceRuntime(setOf("smollm2-360m-instruct"))
        val prefs = FakeUserPreferencesRepository("smollm2-360m-instruct")
        val resolver = PetModelResolver(
            runtime,
            prefs,
            object : ModelAvailabilityGate {
                override suspend fun hasUsableModel(): Boolean = true
                override suspend fun firstUsableModelId(): String = "smollm2-360m-instruct"
            },
        )
        return PetTalkChatRecorder(
            chatRepository = chatRepo,
            petModelResolver = resolver,
            userPreferencesRepository = prefs,
        )
    }
}

private class FakeChatRepository : ChatRepository {
    val conversations = mutableListOf<Conversation>()
    val messages = mutableListOf<ChatMessage>()
    private var nextId = 1L

    override fun observeConversations(): Flow<List<Conversation>> = flowOf(conversations)

    override fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        flowOf(messages.filter { it.conversationId == conversationId })

    override suspend fun createConversation(modelId: String, title: String): Long {
        val id = nextId++
        conversations.add(
            Conversation(
                id = id,
                title = title,
                modelId = modelId,
                createdAtMillis = 0L,
                updatedAtMillis = 0L,
            ),
        )
        return id
    }

    override suspend fun findConversationByTitle(title: String): Conversation? =
        conversations.firstOrNull { it.title == title }

    override suspend fun getOrCreateConversation(title: String, modelId: String): Long {
        val existing = findConversationByTitle(title)
        if (existing != null) return existing.id
        return createConversation(modelId, title)
    }

    override suspend fun getRecentMessagesForTitle(
        title: String,
        modelId: String,
        limit: Int,
    ): List<ChatMessage> {
        val conversation = findConversationByTitle(title) ?: return emptyList()
        return messages.filter { it.conversationId == conversation.id }.takeLast(limit)
    }

    override suspend fun saveMessage(message: ChatMessage) {
        messages.add(message.copy(id = messages.size.toLong() + 1))
    }

    override suspend fun updateConversation(conversation: Conversation) {
        val index = conversations.indexOfFirst { it.id == conversation.id }
        if (index >= 0) conversations[index] = conversation
    }

    override suspend fun deleteMessagesForConversation(conversationId: Long) {
        messages.removeAll { it.conversationId == conversationId }
    }

    override suspend fun deleteAllConversations() {
        conversations.clear()
        messages.clear()
    }
}

private class FakeUserPreferencesRepository(
    private val petModel: String?,
) : UserPreferencesRepository {
    override val defaultModelId = flowOf(null as String?)
    override val petModelId = flowOf(petModel)
    override val generationParams = flowOf(com.nibbli.nibbligo.core.model.GenerationParams())
    override val chatPromptMode = flowOf(ChatPromptMode.PIXEL_FRIEND)
    override val allowDownloads = flowOf(true)
    override val preferredRuntimeKind = flowOf("litert")
    override val petPersonality = flowOf(PetPersonality.PLAYFUL)
    override val usePetLlmReactions = flowOf(true)
    override val petCommentOnAgentWork = flowOf(true)
    override val petMoodPulseMode = flowOf(com.nibbli.nibbligo.core.model.PetMoodPulseMode.NORMAL)
    override val themeMode = flowOf(com.nibbli.nibbligo.core.model.AppThemeMode.SYSTEM)
    override val accentPalette = flowOf(com.nibbli.nibbligo.core.model.AppAccentPalette.TEAL)
    override val showDoTab = flowOf(false)
    override val litertAccelerator = flowOf(com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference.AUTO)
    override val petOnboardingProfile = flowOf(com.nibbli.nibbligo.core.model.PetOnboardingProfile(completed = true))
    override val onboardingCompleted = flowOf(true)
    override val modelSetupPromptDismissed = flowOf(false)
    override val termsAccepted = flowOf(false)
    override val petSoundHapticsEnabled = flowOf(true)
    override val petNotificationsEnabled = flowOf(true)
    override val lcdCoachMarksDismissed = flowOf(false)
    override val firstTalkGreetingSent = flowOf(false)
    override suspend fun getPetNotificationsEnabled(): Boolean = true
    override suspend fun setPetSoundHapticsEnabled(enabled: Boolean) = Unit
    override suspend fun setPetNotificationsEnabled(enabled: Boolean) = Unit
    override suspend fun setLcdCoachMarksDismissed(dismissed: Boolean) = Unit
    override suspend fun setFirstTalkGreetingSent(sent: Boolean) = Unit
    override suspend fun setDefaultModelId(modelId: String?) = Unit
    override suspend fun setPetModelId(modelId: String?) = Unit
    override suspend fun setGenerationParams(params: com.nibbli.nibbligo.core.model.GenerationParams) = Unit
    override suspend fun setChatPromptMode(mode: ChatPromptMode) = Unit
    override suspend fun setAllowDownloads(allowed: Boolean) = Unit
    override suspend fun setPreferredRuntimeKind(kind: String) = Unit
    override suspend fun setPetPersonality(personality: PetPersonality) = Unit
    override suspend fun setUsePetLlmReactions(enabled: Boolean) = Unit
    override suspend fun setPetCommentOnAgentWork(enabled: Boolean) = Unit
    override suspend fun setPetMoodPulseMode(mode: com.nibbli.nibbligo.core.model.PetMoodPulseMode) = Unit
    override suspend fun setThemeMode(mode: com.nibbli.nibbligo.core.model.AppThemeMode) = Unit
    override suspend fun setAccentPalette(palette: com.nibbli.nibbligo.core.model.AppAccentPalette) = Unit
    override suspend fun setShowDoTab(show: Boolean) = Unit
    override suspend fun setPetOnboardingProfile(profile: com.nibbli.nibbligo.core.model.PetOnboardingProfile) = Unit
    override suspend fun setModelSetupPromptDismissed(dismissed: Boolean) = Unit
    override suspend fun setTermsAccepted(acceptedAtMillis: Long) = Unit
    override suspend fun setLitertAccelerator(preference: com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference) = Unit
}
