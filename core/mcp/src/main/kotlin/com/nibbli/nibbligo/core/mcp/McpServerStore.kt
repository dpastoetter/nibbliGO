package com.nibbli.nibbligo.core.mcp

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.mcpStore by preferencesDataStore("mcp_servers")

@Singleton
class McpServerStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("servers_json")
    private val json = Json { ignoreUnknownKeys = true }

    val servers: Flow<List<McpServerConfig>> = context.mcpStore.data.map { prefs ->
        val raw = prefs[key] ?: return@map emptyList()
        json.decodeFromString<McpServerList>(raw).servers
    }

    suspend fun save(servers: List<McpServerConfig>) {
        context.mcpStore.edit {
            it[key] = json.encodeToString(McpServerList(servers))
        }
    }

    suspend fun add(server: McpServerConfig) {
        val prefs = context.mcpStore.data.first()
        val raw = prefs[key]
        val current = if (raw != null) json.decodeFromString<McpServerList>(raw).servers else emptyList()
        save(current.filter { it.id != server.id } + server)
    }
}
