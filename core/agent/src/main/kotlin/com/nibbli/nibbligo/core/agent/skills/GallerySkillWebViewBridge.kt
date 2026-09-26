// Ported from gallery@main: GalleryWebView.kt + WebViewAssetLoader (Apache 2.0)
package com.nibbli.nibbligo.core.agent.skills

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class GallerySkillWebViewBridge @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun runSkillTool(
        skillId: String,
        toolName: String,
        argumentsJson: String,
        allowNetwork: Boolean,
    ): String = suspendCancellableCoroutine { cont ->
        val safeSkillId = sanitizeSkillId(skillId)
            ?: run {
                cont.resume("""{"ok":false,"error":"invalid_skill_id"}""")
                return@suspendCancellableCoroutine
            }
        val safeToolName = sanitizeToolName(toolName)
            ?: run {
                cont.resume("""{"ok":false,"error":"invalid_tool_name"}""")
                return@suspendCancellableCoroutine
            }
        val safeArgsJson = sanitizeArgumentsJson(argumentsJson)

        val skillsRoot = File(context.filesDir, "skills").canonicalFile
        val skillDir = File(skillsRoot, safeSkillId).canonicalFile
        if (!skillDir.path.startsWith(skillsRoot.path + File.separator) && skillDir != skillsRoot) {
            cont.resume("""{"ok":false,"error":"skill_path_escape"}""")
            return@suspendCancellableCoroutine
        }

        val webView = WebView(context.applicationContext)
        cont.invokeOnCancellation {
            webView.destroy()
        }

        // Mount only this skill's directory — never the whole filesDir (HF token, PIN prefs, etc.).
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/skills/$safeSkillId/",
                WebViewAssetLoader.InternalStoragePathHandler(context, skillDir),
            )
            .build()

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.blockNetworkLoads = !allowNetwork
        webView.settings.blockNetworkImage = !allowNetwork

        val bridge = object {
            @JavascriptInterface
            fun getToolName(): String = safeToolName

            @JavascriptInterface
            fun getArgumentsJson(): String = safeArgsJson

            @JavascriptInterface
            fun postToolResult(json: String) {
                if (cont.isActive) cont.resume(json)
            }
        }
        webView.addJavascriptInterface(bridge, "GallerySkill")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val uri = request?.url ?: return blockedResponse()
                val host = uri.host
                if (host == null || host != "appassets.androidplatform.net") {
                    return if (allowNetwork) null else blockedResponse()
                }
                return assetLoader.shouldInterceptRequest(uri) ?: blockedResponse()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // Never interpolate LLM-controlled strings into JS source.
                val script = """
                  (function() {
                    var name = GallerySkill.getToolName();
                    var args = JSON.parse(GallerySkill.getArgumentsJson());
                    if (typeof runTool === 'function') {
                      runTool(name, args);
                    } else {
                      GallerySkill.postToolResult(JSON.stringify({ok:true,tool:name}));
                    }
                  })();
                """.trimIndent()
                view?.evaluateJavascript(script, null)
            }
        }

        val indexHtml = File(skillDir, "scripts/index.html")
        if (indexHtml.isFile) {
            webView.loadUrl("$GALLERY_LOCAL_URL_BASE/skills/$safeSkillId/scripts/index.html")
        } else {
            val jsFile = File(skillDir, "scripts/$safeToolName.js")
            val js = jsFile.takeIf { it.isFile && isUnder(skillDir, it) }?.readText()
                ?: defaultRunToolJs()
            val html = """<html><body><script>$js</script></body></html>"""
            webView.loadDataWithBaseURL(
                "$GALLERY_LOCAL_URL_BASE/skills/$safeSkillId/",
                html,
                "text/html",
                "UTF-8",
                null,
            )
        }
    }

    private fun defaultRunToolJs() = """
        function runTool(name, args) {
          GallerySkill.postToolResult(JSON.stringify({ ok: true, tool: name, args: args }));
        }
    """.trimIndent()

    private fun blockedResponse(): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "utf-8",
            403,
            "Forbidden",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0)),
        )

    companion object {
        private val SKILL_ID_PATTERN = Regex("^[a-zA-Z0-9_-]{1,64}$")
        private val TOOL_NAME_PATTERN = Regex("^[a-zA-Z0-9_.-]{1,64}$")

        internal fun sanitizeSkillId(skillId: String): String? =
            skillId.takeIf { SKILL_ID_PATTERN.matches(it) }

        internal fun sanitizeToolName(toolName: String): String? =
            toolName.takeIf { TOOL_NAME_PATTERN.matches(it) }

        internal fun sanitizeArgumentsJson(argumentsJson: String): String {
            return try {
                JSONObject(argumentsJson.ifBlank { "{}" }).toString()
            } catch (_: Exception) {
                "{}"
            }
        }

        private fun isUnder(root: File, candidate: File): Boolean {
            val rootPath = root.canonicalFile.path
            val candidatePath = candidate.canonicalFile.path
            return candidatePath == rootPath || candidatePath.startsWith(rootPath + File.separator)
        }
    }
}
