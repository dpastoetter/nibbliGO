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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "LiteRtEnginePool"

data class LiteRtSession(
    val engine: Engine,
    var conversation: Conversation,
)

@Singleton
class LiteRtEnginePool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedModelPathResolver: InstalledModelPathResolver,
) {
    private val sessions = ConcurrentHashMap<String, LiteRtSession>()

    fun modelPath(modelId: String): File? =
        installedModelPathResolver.resolveFile(modelId)
            ?.takeIf { it.name.endsWith(".litertlm", ignoreCase = true) && it.length() > 1_000_000 }

    fun hasModel(modelId: String): Boolean = modelPath(modelId) != null

    suspend fun ensureSession(
        modelId: String,
        tools: List<ToolProvider> = emptyList(),
        systemInstruction: String? = null,
    ): LiteRtSession {
        sessions[modelId]?.let { return it }
        val path = modelPath(modelId)?.absolutePath
            ?: error("No .litertlm file for $modelId")
        val backend: Backend = Backend.GPU()
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
        val session = LiteRtSession(engine, conversation)
        sessions[modelId] = session
        return session
    }

    fun unload(modelId: String) {
        sessions.remove(modelId)?.let { session ->
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
                    val thought = message.channels["thought"]?.toString()
                    trySend(StreamEvent.Token(message.toString(), thought))
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
    ): TurnResult {
        val session = ensureSession(modelId, tools)
        return suspendCancellableCoroutine { cont ->
        val builder = StringBuilder()
        val thoughts = mutableListOf<String>()
        session.conversation.sendMessageAsync(
            Contents.of(Content.Text(userText)),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    builder.append(message.toString())
                    message.channels["thought"]?.toString()?.let { thoughts.add(it) }
                }

                override fun onDone() {
                    cont.resume(TurnResult(builder.toString(), thoughts))
                }

                override fun onError(throwable: Throwable) {
                    cont.resumeWithException(throwable)
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

data class TurnResult(val text: String, val thinkingTrace: List<String>)
