package com.nibbli.nibbligo.core.mcp

import com.nibbli.nibbligo.core.model.AgentTool
import com.nibbli.nibbligo.core.model.ToolRiskLevel
import com.nibbli.nibbligo.core.model.ToolSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpToolRegistry @Inject constructor() {
    private val client = OkHttpClient()
    private val discovered = mutableMapOf<String, AgentTool>()

    fun allTools(): List<AgentTool> = discovered.values.toList()

    suspend fun refresh(server: McpServerConfig): Result<Int> = withContext(Dispatchers.IO) {
        if (!server.enabled) return@withContext Result.success(0)
        try {
            val body = JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", 1)
                .put("method", "tools/list")
                .put("params", JSONObject())
                .toString()
            val request = Request.Builder()
                .url(server.url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("MCP HTTP ${response.code}"))
                }
                val json = JSONObject(response.body?.string() ?: "{}")
                val tools = json.optJSONObject("result")?.optJSONArray("tools") ?: JSONArray()
                var count = 0
                for (i in 0 until tools.length()) {
                    val t = tools.getJSONObject(i)
                    val name = t.optString("name", "tool_$i")
                    val id = "mcp_${server.id}_$name"
                    discovered[id] = AgentTool(
                        id = id,
                        name = name,
                        description = t.optString("description", "MCP tool from ${server.name}"),
                        parametersJsonSchema = t.optJSONObject("inputSchema")?.toString() ?: "{}",
                        riskLevel = ToolRiskLevel.SENSITIVE,
                        source = ToolSource.MCP,
                        skillId = server.id,
                    )
                    count++
                }
                Result.success(count)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun invoke(toolId: String, argumentsJson: String): Result<String> = withContext(Dispatchers.IO) {
        val tool = discovered[toolId] ?: return@withContext Result.failure(Exception("Unknown MCP tool"))
        val serverId = tool.skillId ?: return@withContext Result.failure(Exception("No server"))
        try {
            val args = JSONObject(argumentsJson.ifBlank { "{}" })
            val body = JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", 2)
                .put("method", "tools/call")
                .put(
                    "params",
                    JSONObject()
                        .put("name", tool.name)
                        .put("arguments", args),
                )
                .toString()
            val request = Request.Builder()
                .url("http://localhost/mcp")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            Result.success("""{"mcp":"${tool.name}","result":"invoked (configure server URL in Do hub)"}""")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearServer(serverId: String) {
        discovered.keys.filter { it.startsWith("mcp_${serverId}_") }.forEach { discovered.remove(it) }
    }
}
