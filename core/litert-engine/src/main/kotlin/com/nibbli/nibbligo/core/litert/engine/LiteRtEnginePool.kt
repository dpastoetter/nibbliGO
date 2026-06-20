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
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.model.GenerationParams
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
    val petModelId: String = "",
    var petSystemInstruction: String? = null,
    val petTools: List<ToolProvider> = emptyList(),
    val homeTalkModelId: String = "",
    var homeTalkSystemInstruction: String? = null,
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
    @Volatile
    private var cachedGenerationParams = GenerationParams()

    private fun sessionKey(
        modelId: String,
        tools: List<ToolProvider>,
        systemInstruction: String? = null,
        profile: String = SESSION_PROFILE_DEFAULT,
    ): String {
        val toolsPart = if (tools.isEmpty()) "plain" else "tools"
        val sysPart = when {
            profile == SESSION_PROFILE_PET_CHAT -> "pet-static"
            profile == SESSION_PROFILE_HOME_TALK -> "home-talk-static"
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
        sessions[key]?.let { existing ->
            if (
                profile == SESSION_PROFILE_PET_CHAT &&
                systemInstruction != null &&
                systemInstruction != existing.petSystemInstruction
            ) {
                existing.petSystemInstruction = systemInstruction
                refreshPetConversation(existing)
            }
            if (
                profile == SESSION_PROFILE_HOME_TALK &&
                systemInstruction != null &&
                systemInstruction != existing.homeTalkSystemInstruction
            ) {
                existing.homeTalkSystemInstruction = systemInstruction
                refreshHomeTalkConversation(existing)
            }
            return existing
        }

        return initMutex.withLock {
            sessions[key]?.let { existing ->
                if (
                    profile == SESSION_PROFILE_PET_CHAT &&
                    systemInstruction != null &&
                    systemInstruction != existing.petSystemInstruction
                ) {
                    existing.petSystemInstruction = systemInstruction
                    refreshPetConversation(existing)
                }
                if (
                    profile == SESSION_PROFILE_HOME_TALK &&
                    systemInstruction != null &&
                    systemInstruction != existing.homeTalkSystemInstruction
                ) {
                    existing.homeTalkSystemInstruction = systemInstruction
                    refreshHomeTalkConversation(existing)
                }
                return@withLock existing
            }

            // One Engine per model — avoid loading GPU weights multiple times (critical on Pixel).
            if (
                profile == SESSION_PROFILE_PET_CHAT ||
                profile == SESSION_PROFILE_HOME_TALK ||
                profile == SESSION_PROFILE_DEFAULT
            ) {
                unloadSessionsForModel(modelId, exceptKey = key)
            }

            val path = modelPath(modelId)?.absolutePath
                ?: throw ModelFileNotFoundException("No .litertlm file for $modelId")

            val preference = userPreferencesRepository.litertAccelerator.first()
            cachedGenerationParams = userPreferencesRepository.generationParams.first()
            val backends = LiteRtBackendResolver.resolveBackendOrder(
                modelId = modelId,
                userPreference = preference,
                nativeLibraryDir = context.applicationInfo.nativeLibraryDir,
            )

            var lastError: Exception? = null
            val attempted = backends.map { it.name }
            for (backend in backends) {
                try {
                    val ensureStart = System.nanoTime()
                    val session = createSession(path, modelId, backend, tools, systemInstruction, profile)
                    LiteRtPetTiming.log("ensureSession", (System.nanoTime() - ensureStart) / 1_000_000)
                    val fallbackNote = if (backend.name != attempted.firstOrNull()) {
                        " (after ${attempted.takeWhile { it != backend.name }.joinToString("→") { it }} failed)"
                    } else {
                        ""
                    }
                    Log.i(
                        TAG,
                        "Loaded $modelId on ${backend.name} backend$fallbackNote " +
                            "(tools=${tools.isNotEmpty()}, profile=$profile, pref=${preference.name.lowercase()})",
                    )
                    sessions[key] = session
                    return@withLock session
                } catch (e: Exception) {
                    Log.w(
                        TAG,
                        "Backend ${backend.name} failed for $modelId: ${e.message ?: e.javaClass.simpleName}",
                        e,
                    )
                    lastError = e
                    sessions.remove(key)?.let { closeSession(it) }
                }
            }
            Log.e(
                TAG,
                "All LiteRT backends failed for $modelId (tried ${attempted.joinToString("→")}). " +
                    "On API 31+ devices, GPU needs OpenCL vendor libs declared in AndroidManifest.",
            )
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
        modelId: String,
        backend: Backend,
        tools: List<ToolProvider>,
        systemInstruction: String?,
        profile: String = SESSION_PROFILE_DEFAULT,
    ): LiteRtSession {
        val modelFile = File(path)
        val catalogMaxTokens = ModelCatalog.find(modelId)?.maxContextTokens
        val maxNumTokens = when (profile) {
            SESSION_PROFILE_PET_CHAT -> PET_MAX_NUM_TOKENS
            SESSION_PROFILE_HOME_TALK -> HOME_TALK_MAX_NUM_TOKENS
            else -> catalogMaxTokens ?: DEFAULT_MAX_NUM_TOKENS
        }
        // maxNumTokens is total context capacity (input + output), not output length alone.
        val cacheDir = context.getExternalFilesDir(null)?.absolutePath
            ?: modelFile.parentFile?.absolutePath
        val engineConfig = EngineConfig(
            modelPath = path,
            backend = backend,
            maxNumTokens = maxNumTokens,
            cacheDir = cacheDir,
        )
        val engine = Engine(engineConfig)
        engine.initialize()
        val sysContents = systemInstruction?.let { Contents.of(Content.Text(it)) }
        val samplerConfig = samplerConfigFor(profile, modelId)
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
            petModelId = if (profile == SESSION_PROFILE_PET_CHAT) modelId else "",
            petSystemInstruction = petMeta?.first,
            petTools = petMeta?.second ?: emptyList(),
            homeTalkModelId = if (profile == SESSION_PROFILE_HOME_TALK) modelId else "",
            homeTalkSystemInstruction = if (profile == SESSION_PROFILE_HOME_TALK) systemInstruction else null,
        )
    }

    private fun samplerConfigFor(profile: String, modelId: String): SamplerConfig {
        val defaults = samplerDefaults(profile, modelId)
        if (profile == SESSION_PROFILE_MOBILE_ACTIONS) return defaults
        val params = cachedGenerationParams
        return SamplerConfig(
            topK = params.topK.coerceIn(1, 128),
            topP = params.topP.coerceIn(0.01f, 1f).toDouble(),
            temperature = when (profile) {
                SESSION_PROFILE_PET_CHAT, SESSION_PROFILE_HOME_TALK -> defaults.temperature
                else -> params.temperature.coerceIn(0f, 2f).toDouble()
            },
        )
    }

    private fun samplerDefaults(profile: String, modelId: String): SamplerConfig = when {
        profile == SESSION_PROFILE_PET_CHAT && isCompactPetModel(modelId) ->
            SamplerConfig(topK = 16, topP = 0.9, temperature = 0.4)
        profile == SESSION_PROFILE_PET_CHAT ->
            SamplerConfig(topK = 24, topP = 0.92, temperature = 0.7)
        profile == SESSION_PROFILE_HOME_TALK ->
            SamplerConfig(topK = 24, topP = 0.9, temperature = 0.5)
        profile == SESSION_PROFILE_MOBILE_ACTIONS ->
            SamplerConfig(topK = 64, topP = 0.95, temperature = 0.0)
        else -> SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8)
    }

    private fun isCompactPetModel(modelId: String): Boolean =
        modelId.contains("smollm", ignoreCase = true) ||
            modelId.contains("functiongemma", ignoreCase = true)

    internal fun refreshPetConversationTimed(session: LiteRtSession): Long {
        val start = System.nanoTime()
        refreshPetConversation(session)
        return (System.nanoTime() - start) / 1_000_000
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
                samplerConfig = samplerConfigFor(SESSION_PROFILE_PET_CHAT, session.petModelId),
                systemInstruction = sysContents,
                tools = session.petTools,
            ),
        )
    }

    /** Drop accumulated Home talk turns while keeping cached system instruction. */
    fun resetHomeTalkSession(modelId: String) {
        val key = sessionKey(modelId, emptyList(), null, SESSION_PROFILE_HOME_TALK)
        sessions[key]?.let { session ->
            refreshHomeTalkConversation(session)
        }
    }

    private fun refreshHomeTalkConversation(session: LiteRtSession) {
        if (session.homeTalkSystemInstruction == null) return
        try {
            session.conversation.close()
        } catch (e: Exception) {
            Log.w(TAG, "home talk conversation close before refresh", e)
        }
        val sysContents = session.homeTalkSystemInstruction?.let { Contents.of(Content.Text(it)) }
        session.conversation = session.engine.createConversation(
            ConversationConfig(
                samplerConfig = samplerConfigFor(SESSION_PROFILE_HOME_TALK, session.homeTalkModelId),
                systemInstruction = sysContents,
            ),
        )
    }

    /** Drop accumulated chat turns for the plain (Local Chat) session. */
    fun resetPlainChatSession(modelId: String, tools: List<ToolProvider> = emptyList()) {
        val key = sessionKey(modelId, tools, null, SESSION_PROFILE_DEFAULT)
        sessions[key]?.let { session ->
            refreshPlainChatSession(session, modelId, tools)
        }
    }

    private fun refreshPlainChatSession(
        session: LiteRtSession,
        modelId: String,
        tools: List<ToolProvider>,
    ) {
        try {
            session.conversation.close()
        } catch (e: Exception) {
            Log.w(TAG, "plain conversation close before refresh", e)
        }
        session.conversation = session.engine.createConversation(
            ConversationConfig(
                samplerConfig = samplerConfigFor(SESSION_PROFILE_DEFAULT, modelId),
                tools = tools,
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
            val session = ensureSession(modelId, tools, systemInstruction, profile)
            petTurnMutex.withLock {
                callbackFlow {
                    val activeSession = session
                    val contents = Contents.of(Content.Text(userText))
                    activeSession.conversation.sendMessageAsync(
                        contents,
                        object : MessageCallback {
                            override fun onMessage(message: Message) {
                                val chunk = message.extractModelReplyText()
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
            }
            val refreshMs = refreshPetConversationTimed(session)
            LiteRtPetTiming.log("refreshPetConversation", refreshMs)
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
        val key = sessionKey(modelId, tools, systemInstruction, profile)
        val result = petTurnMutex.withLock {
            sendTurnInternal(modelId, userText, tools, systemInstruction, profile)
        }
        sessions[key]?.let { session ->
            val refreshMs = refreshPetConversationTimed(session)
            LiteRtPetTiming.log("refreshPetConversation", refreshMs)
        }
        return result
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
                        val chunk = if (profile == SESSION_PROFILE_PET_CHAT) {
                            message.extractModelReplyText()
                        } else {
                            message.extractDisplayText()
                        }
                        builder.append(chunk)
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

    fun measurePetRefresh(modelId: String): Long {
        val key = sessionKey(modelId, emptyList(), null, SESSION_PROFILE_PET_CHAT)
        val session = sessions[key] ?: return 0L
        return refreshPetConversationTimed(session)
    }

    suspend fun benchmarkStreamTurn(
        modelId: String,
        userText: String,
        systemInstruction: String? = null,
        profile: String = SESSION_PROFILE_DEFAULT,
    ): LiteRtStreamBenchmarkResult {
        val decodeStart = System.nanoTime()
        var firstTokenAt: Long? = null
        var charCount = 0
        streamMessage(modelId, userText, emptyList(), systemInstruction, profile).collect { event ->
            when (event) {
                is StreamEvent.Token -> {
                    if (firstTokenAt == null) {
                        firstTokenAt = System.nanoTime()
                    }
                    charCount += event.text.length
                }
                StreamEvent.Done -> Unit
            }
        }
        val totalMs = (System.nanoTime() - decodeStart) / 1_000_000
        val ttftMs = firstTokenAt?.let { (it - decodeStart) / 1_000_000 } ?: totalMs
        val tokenEstimate = (charCount / 4).coerceAtLeast(1)
        val decodeMs = (totalMs - ttftMs).coerceAtLeast(1)
        val tokPerSec = tokenEstimate * 1000f / decodeMs
        return LiteRtStreamBenchmarkResult(
            timeToFirstTokenMs = ttftMs,
            totalMs = totalMs,
            approximateTokenCount = tokenEstimate,
            tokensPerSecond = tokPerSec,
        )
    }

    companion object {
        const val SESSION_PROFILE_DEFAULT = "default"
        const val SESSION_PROFILE_PET_CHAT = "pet-chat"
        const val SESSION_PROFILE_HOME_TALK = "home-talk"
        const val SESSION_PROFILE_MOBILE_ACTIONS = "mobile-actions"
        private const val DEFAULT_MAX_NUM_TOKENS = 2048
        internal const val PET_MAX_NUM_TOKENS = 1024
        internal const val HOME_TALK_MAX_NUM_TOKENS = 512
        const val BENCHMARK_RAW_PROMPT = "Say hello in one short sentence."
        const val BENCHMARK_HOME_TALK_FAST_PROMPT = "Caretaker: Hi there!"
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
