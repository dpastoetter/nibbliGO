// Ported from gallery@main: ProjectConfig.kt (Apache 2.0)
package com.nibbli.nibbligo.core.hf.download

/**
 * Hugging Face OAuth — set in local.properties:
 * hf.oauth.clientId=...
 * hf.oauth.redirectUri=nibbli://oauth/huggingface
 */
object HuggingFaceConfig {
    const val DEFAULT_REDIRECT_URI = "nibbli://oauth/huggingface"
    const val AUTH_ENDPOINT = "https://huggingface.co/oauth/authorize"
    const val TOKEN_ENDPOINT = "https://huggingface.co/oauth/token"
}
