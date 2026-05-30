// Ported from gallery@main: LlmChatModelHelper.kt (Apache 2.0)
package com.nibbli.nibbligo.core.litert.engine

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ToolProvider
import com.nibbli.nibbligo.core.domain.model.InstalledModelPathResolver
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "LiteRtEnginePool"

data class LiteRtSession(
    val engine: Engine,
    var conversation: Conversation,
    val backendName: String,
    val petSystemInstruction: String? = null,
    val petTools: List<ToolProvider> = emptyList(),
)

@Singleton
class LiteRtEnginePool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedModelPathResolver: InstalledModelPathResolver,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private val sessions = ConcurrentHashMap<String, LiteRtSession>()
    private val initMutex = Mutex()
    private val petTurnMutex = Mutex()

    private fun sessionKey(
        modelId: String,
        tools: List<ToolProvider>,
        systemInstruction: String? = null,
        profile: String = SESSION_PROFILE_DEFAULT,
    ): String {
        val toolsPart = if (tools.isEmpty()) "plain" else "tools"
        val sysPart = when {
            profile == SESSION_PROFILE_PET_CHAT -> "pet-static"
            profile == SESSION_PROFILE_MOBILE_ACTIONS -> "mobile-static"
            else -> systemInstruction?.hashCode()?.toString() ?: "nosys"
        }
        return "$modelId@$toolsPart@$profile@$sysPart"
    }

    fun modelPath(modelId: String): File? =
        installedModelPathResolver.resolveFile(modelId)
            ?.takeIf { it.name.endsWith(".litertlm", ignoreCase = true) && it.length() > 1_000_000 }

    fun hasModel(modelId: String): Boolean = modelPath(modelId) != null

    suspend fun ensureSession(
        modelId: String,
        tools: List<ToolProvider> = emptyList(),
        systemInstruction: String? = null,
        profile: String = SESSION_PROFILE_DEFAULT,
    ): LiteRtSession {
        val key = sessionKey(modelId, tools, systemInstruction, profile)
        sessions[key]?.let { return it }

        return initMutex.withLock {
            sessions[key]?.let { return it }

            // Pet chat and agent paths must not keep two full Engine instances for one model.
            if (profile == SESSION_PROFILE_PET_CHAT) {
                unloadSessionsForModel(modelId, exceptKey = key)
            }

            val path = modelPath(modelId)?.absolutePath
                ?: error("No .litertlm file for $modelId")

            val preference = userPreferencesRepository.litertAccelerator.first()
            val backends = LiteRtBackendResolver.resolveBackendOrder(
                modelId = modelId,
                userPreference = preference,
                nativeLibraryDir = context.applicationInfo.nativeLibraryDir,
            )

            var lastError: Exception? = null
            for (backend in backends) {
                try {
                    val session = createSession(path, backend, tools, systemInstruction, profile)
                    Log.i(
                        TAG,
                        "Loaded $modelId on ${backend.name} backend " +
                            "(tools=${tools.isNotEmpty()}, profile=$profile, pref=${preference.name.lowercase()})",
                    )
                    sessions[key] = session
                    return@withLock session
                } catch (e: Exception) {
                    Log.w(TAG, "Backend ${backend.name} failed for $modelId", e)
                    lastError = e
                    sessions.remove(key)?.let { closeSession(it) }
                }
            }
            throw lastError ?: IllegalStateException("Failed to load LiteRT model $modelId")
        }
    }

    private fun unloadSessionsForModel(modelId: String, exceptKey: String? = null) {
        val prefix = "$modelId@"
        sessions.keys.filter { it.startsWith(prefix) && it != exceptKey }.forEach { key ->
            sessions.remove(key)?.let { closeSession(it) }
        }
    }

    private fun createSession(
        path: String,
        backend: Backend,
        tools: List<ToolProvider>,
        systemInstruction: String?,
        profile: String = SESSION_PROFILE_DEFAULT,
    ): LiteRtSession {
        val modelFile = File(path)
        // maxNumTokens is total context capacity (input + output), not output length alone.
        val engineConfig = EngineConfig(
            modelPath = path,
            backend = backend,
            maxNumTokens = DEFAULT_MAX_NUM_TOKENS,
            cacheDir = modelFile.parentFile?.absolutePath,
        )
        val engine = Engine(engineConfig)
        engine.initialize()
        val sysContents = systemInstruction?.let { Contents.of(Content.Text(it)) }
        val samplerConfig = samplerConfigFor(profile)
        val conversation = engine.createConversation(
            ConversationConfig(
                samplerConfig = samplerConfig,
                systemInstruction = sysContents,
                tools = tools,
            ),
        )
        val petMeta = if (profile == SESSION_PROFILE_PET_CHAT) {
            systemInstruction to tools
        } else {
            null
        }
        return LiteRtSession(
            engine = engine,
            conversation = conversation,
            backendName = backend.name,
            petSystemInstruction = petMeta?.first,
            petTools = petMeta?.second ?: emptyList(),
        )
    }

    private fun samplerConfigFor(profile: String): SamplerConfig = when (profile) {
        SESSION_PROFILE_PET_CHAT -> SamplerConfig(topK = 24, topP = 0.92, temperature = 0.7)
        SESSION_PROFILE_MOBILE_ACTIONS -> SamplerConfig(topK = 64, topP = 0.95, temperature = 0.0)
        else -> SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8)
    }

    private fun refreshPetConversation(session: LiteRtSession) {
        if (session.petSystemInstruction == null && session.petTools.isEmpty()) return
        try {
            session.conversation.close()
        } catch (e: Exception) {
            Log.w(TAG, "pet conversation close before refresh", e)
        }
        val sysContents = session.petSystemInstruction?.let { Contents.of(Content.Text(it)) }
        session.conversation = session.engine.createConversation(
            ConversationConfig(
                samplerConfig = samplerConfigFor(SESSION_PROFILE_PET_CHAT),
                systemInstruction = sysContents,
                tools = session.petTools,
            ),
        )
    }

    fun unload(modelId: String) {
        val keys = sessions.keys.filter { it.startsWith("$modelId@") }
        keys.forEach { key ->
            sessions.remove(key)?.let { closeSession(it) }
        }
    }

    fun unloadAll() {
        sessions.keys.toList().forEach { key ->
            sessions.remove(key)?.let { closeSession(it) }
        }
    }

    fun activeBackendFor(modelId: String): String? =
        sessions.entries
            .firstOrNull { it.key.startsWith("$modelId@") }
            ?.value
            ?.backendName

    private fun closeSession(session: LiteRtSession) {
        try {
            session.conversation.close()
        } catch (e: Exception) {
            Log.w(TAG, "conversation close", e)
        }
        try {
            session.engine.close()
        } catch (e: Exception) {
            Log.w(TAG, "engine close", e)
        }
    }

    fun streamMessage(
        modelId: String,
        userText: String,
        tools: List<ToolProvider> = emptyList(),
        systemInstruction: String? = null,
        profile: String = SESSION_PROFILE_DEFAULT,
    ): Flow<StreamEvent> {
        if (profile != SESSION_PROFILE_PET_CHAT) {
            return streamMessageInternal(modelId, userText, tools, systemInstruction, profile)
        }
        return flow {
            petTurnMutex.withLock {
                var session: LiteRtSession? = null
                callbackFlow {
                    session = ensureSession(modelId, tools, systemInstruction, profile)
                    val activeSession = session!!
                    val contents = Contents.of(Content.Text(userText))
                    activeSession.conversation.sendMessageAsync(
                        contents,
                        object : MessageCallback {
                            override fun onMessage(message: Message) {
                                val chunk = message.extractDisplayText()
                                if (chunk.isNotEmpty()) {
                                    trySend(StreamEvent.Token(chunk, message.extractThought()))
                                }
                            }

                            override fun onDone() {
                                trySend(StreamEvent.Done)
                                close()
                            }

                            override fun onError(throwable: Throwable) {
                                close(throwable)
                            }
                        },
                        emptyMap(),
                    )
                    awaitClose { activeSession.conversation.cancelProcess() }
                }.collect { emit(it) }
                session?.let { refreshPetConversation(it) }
            }
        }
    }

    private fun streamMessageInternal(
        modelId: String,
        userText: String,
        tools: List<ToolProvider>,
        systemInstruction: String?,
        profile: String,
    ): Flow<StreamEvent> = callbackFlow {
        val session = ensureSession(modelId, tools, systemInstruction, profile)
        val contents = Contents.of(Content.Text(userText))
        session.conversation.sendMessageAsync(
            contents,
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    val chunk = message.extractDisplayText()
                    if (chunk.isNotEmpty()) {
                        trySend(StreamEvent.Token(chunk, message.extractThought()))
                    }
                }

                override fun onDone() {
                    trySend(StreamEvent.Done)
                    close()
                }

                override fun onError(throwable: Throwable) {
                    close(throwable)
                }
            },
            emptyMap(),
        )
        awaitClose { session.conversation.cancelProcess() }
    }

    suspend fun sendTurn(
        modelId: String,
        userText: String,
        tools: List<ToolProvider> = emptyList(),
        systemInstruction: String? = null,
        profile: String = SESSION_PROFILE_DEFAULT,
    ): TurnResult {
        if (profile != SESSION_PROFILE_PET_CHAT) {
            return sendTurnInternal(modelId, userText, tools, systemInstruction, profile)
        }
        return petTurnMutex.withLock {
            val key = sessionKey(modelId, tools, systemInstruction, profile)
            val result = sendTurnInternal(modelId, userText, tools, systemInstruction, profile)
            sessions[key]?.let { refreshPetConversation(it) }
            result
        }
    }

    private suspend fun sendTurnInternal(
        modelId: String,
        userText: String,
        tools: List<ToolProvider>,
        systemInstruction: String?,
        profile: String,
    ): TurnResult {
        val session = ensureSession(modelId, tools, systemInstruction, profile)
        return suspendCancellableCoroutine { cont ->
            val builder = StringBuilder()
            val thoughts = mutableListOf<String>()
            val toolCalls = mutableListOf<com.google.ai.edge.litertlm.ToolCall>()
            session.conversation.sendMessageAsync(
                Contents.of(Content.Text(userText)),
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        builder.append(message.extractDisplayText())
                        message.extractThought()?.let { thoughts.add(it) }
                        if (message.toolCalls.isNotEmpty()) {
                            toolCalls.addAll(message.toolCalls)
                        }
                    }

                    override fun onDone() {
                        cont.resume(TurnResult(builder.toString(), thoughts, toolCalls))
                    }

                    override fun onError(throwable: Throwable) {
                        cont.resumeWithException(throwable)
                    }
                },
                emptyMap(),
            )
        }
    }

    /**
     * Agent turn: stop as soon as the model proposes tool calls so the UI can confirm before execution.
     */
    suspend fun sendAgentTurn(
        modelId: String,
        userText: String,
        tools: List<ToolProvider>,
        systemInstruction: String?,
    ): TurnResult {
        val session = ensureSession(modelId, tools, systemInstruction, SESSION_PROFILE_MOBILE_ACTIONS)
        return suspendCancellableCoroutine { cont ->
            val builder = StringBuilder()
            val thoughts = mutableListOf<String>()
            val toolCalls = mutableListOf<com.google.ai.edge.litertlm.ToolCall>()
            var finished = false

            fun finish(result: TurnResult) {
                if (finished) return
                finished = true
                cont.resume(result)
            }

            session.conversation.sendMessageAsync(
                Contents.of(Content.Text(userText)),
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        message.extractThought()?.let { thoughts.add(it) }
                        if (message.toolCalls.isNotEmpty()) {
                            toolCalls.addAll(message.toolCalls)
                            builder.append(message.extractDisplayText())
                            session.conversation.cancelProcess()
                            finish(
                                TurnResult(
                                    text = builder.toString(),
                                    thinkingTrace = thoughts,
                                    toolCalls = toolCalls.distinctBy { it.name },
                                ),
                            )
                            return
                        }
                        builder.append(message.extractDisplayText())
                    }

                    override fun onDone() {
                        finish(TurnResult(builder.toString(), thoughts, toolCalls))
                    }

                    override fun onError(throwable: Throwable) {
                        when {
                            finished -> Unit
                            throwable is CancellationException && toolCalls.isNotEmpty() ->
                                finish(TurnResult(builder.toString(), thoughts, toolCalls))
                            cont.isActive -> cont.resumeWithException(throwable)
                        }
                    }
                },
                emptyMap(),
            )
        }
    }

    companion object {
        const val SESSION_PROFILE_DEFAULT = "default"
        const val SESSION_PROFILE_PET_CHAT = "pet-chat"
        const val SESSION_PROFILE_MOBILE_ACTIONS = "mobile-actions"
        private const val DEFAULT_MAX_NUM_TOKENS = 2048
    }
}

sealed class StreamEvent {
    data class Token(val text: String, val thinking: String?) : StreamEvent()
    data object Done : StreamEvent()
}

data class TurnResult(
    val text: String,
    val thinkingTrace: List<String>,
    val toolCalls: List<com.google.ai.edge.litertlm.ToolCall> = emptyList(),
)
