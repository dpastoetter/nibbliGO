// Ported from gallery@main: McpServersSerializer.kt (Apache 2.0)
package com.nibbli.nibbligo.core.mcp

import kotlinx.serialization.Serializable

@Serializable
data class McpServerConfig(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
)

@Serializable
data class McpServerList(
    val servers: List<McpServerConfig> = emptyList(),
)
