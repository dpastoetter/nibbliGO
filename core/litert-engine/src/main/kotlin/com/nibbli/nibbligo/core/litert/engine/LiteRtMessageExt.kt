package com.nibbli.nibbligo.core.litert.engine

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Message

fun Message.extractText(): String =
    contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }

fun Message.extractThought(): String? = channels["thought"]

fun Message.extractToolSummary(): String? =
    toolCalls.takeIf { it.isNotEmpty() }?.joinToString { it.name }?.let { "($it)" }

/** All tool calls emitted in this message chunk. */
fun Message.liteRtToolCalls(): List<com.google.ai.edge.litertlm.ToolCall> = toolCalls

/** Converts LiteRT tool-call argument maps to JSON for agent [ToolCall]. */
fun liteRtToolArgumentsToJson(arguments: Map<String, Any?>): String {
    val json = org.json.JSONObject()
    arguments.forEach { (key, value) ->
        when (value) {
            null -> json.put(key, org.json.JSONObject.NULL)
            is Number -> json.put(key, value)
            is Boolean -> json.put(key, value)
            else -> json.put(key, value.toString())
        }
    }
    return json.toString()
}

/** Text tokens plus a short tool-call summary when the model emits tools without text. */
fun Message.extractDisplayText(): String {
    val text = extractText()
    if (text.isNotBlank()) return text
    return extractToolSummary().orEmpty()
}
