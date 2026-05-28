package com.nibbli.nibbligo.core.hf.download

import android.content.Context

object HuggingFaceOAuthConfigLoader {
    fun load(
        context: Context,
        buildClientId: String = "",
        buildRedirectUri: String = "",
    ): HuggingFaceOAuthConfig {
        if (buildClientId.isNotBlank()) {
            return HuggingFaceOAuthConfig(
                clientId = buildClientId,
                redirectUri = buildRedirectUri.ifBlank { HuggingFaceConfig.DEFAULT_REDIRECT_URI },
            )
        }
        val props = try {
            context.assets.open("hf.oauth.properties").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }
        val fromProps = parseProps(props)
        val clientId = fromProps["clientId"].orEmpty()
        val redirectUri = fromProps["redirectUri"] ?: HuggingFaceConfig.DEFAULT_REDIRECT_URI
        return HuggingFaceOAuthConfig(clientId, redirectUri)
    }

    private fun parseProps(text: String): Map<String, String> =
        text.lines()
            .mapNotNull { line ->
                val t = line.trim()
                if (t.isEmpty() || t.startsWith("#")) return@mapNotNull null
                val idx = t.indexOf('=')
                if (idx < 0) null else t.substring(0, idx).trim() to t.substring(idx + 1).trim()
            }
            .toMap()
}
