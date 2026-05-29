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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
)

@Singleton
class LiteRtEnginePool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedModelPathResolver: InstalledModelPathResolver,
) {
    private val sessions = ConcurrentHashMap<String, LiteRtSession>()

    private fun sessionKey(
        modelId: String,
        tools: List<ToolProvider>,
        systemInstruction: String? = null,
    ): String {
        val toolsPart = if (tools.isEmpty()) "plain" else "tools"
        val sysPart = systemInstruction?.hashCode()?.toString() ?: "nosys"
        return "$modelId@$toolsPart@$sysPart"
    }

    fun modelPath(modelId: String): File? =
        installedModelPathResolver.resolveFile(modelId)
            ?.takeIf { it.name.endsWith(".litertlm", ignoreCase = true) && it.length() > 1_000_000 }

    fun hasModel(modelId: String): Boolean = modelPath(modelId) != null

    suspend fun ensureSession(
        modelId: String,
        tools: List<ToolProvider> = emptyList(),
        systemInstruction: String? = null,
    ): LiteRtSession {
        val key = sessionKey(modelId, tools, systemInstruction)
        sessions[key]?.let { return it }
        val path = modelPath(modelId)?.absolutePath
            ?: error("No .litertlm file for $modelId")

        var lastError: Exception? = null
        for (backend in listOf(Backend.GPU(), Backend.CPU())) {
            try {
                val session = createSession(path, backend, tools, systemInstruction)
                Log.i(TAG, "Loaded $modelId on ${backend.name} backend (tools=${tools.isNotEmpty()})")
                sessions[key] = session
                return session
            } catch (e: Exception) {
                Log.w(TAG, "Backend ${backend.name} failed for $modelId", e)
                lastError = e
                sessions.remove(key)?.let { closeSession(it) }
            }
        }
        throw lastError ?: IllegalStateException("Failed to load LiteRT model $modelId")
    }

    private fun createSession(
        path: String,
        backend: Backend,
        tools: List<ToolProvider>,
        systemInstruction: String?,
    ): LiteRtSession {
        val engineConfig = EngineConfig(
            modelPath = path,
            backend = backend,
            maxNumTokens = 2048,
        )
        val engine = Engine(engineConfig)
        engine.initialize()
        val sysContents = systemInstruction?.let { Contents.of(Content.Text(it)) }
        val conversation = engine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8),
                systemInstruction = sysContents,
                tools = tools,
            ),
        )
        return LiteRtSession(engine, conversation, backend.name)
    }

    fun unload(modelId: String) {
        val keys = sessions.keys.filter { it.startsWith("$modelId@") }
        keys.forEach { key ->
            sessions.remove(key)?.let { closeSession(it) }
        }
    }

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
    ): Flow<StreamEvent> = callbackFlow {
        val session = ensureSession(modelId, tools)
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
    ): TurnResult {
        val session = ensureSession(modelId, tools, systemInstruction)
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
        val session = ensureSession(modelId, tools, systemInstruction)
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
