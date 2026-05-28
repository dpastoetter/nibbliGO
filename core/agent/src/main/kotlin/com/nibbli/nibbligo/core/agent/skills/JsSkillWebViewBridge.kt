package com.nibbli.nibbligo.core.agent.skills

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Hidden WebView host for Gallery-compatible JS skill scripts.
 * Scripts should call `NibbliSkill.postToolResult(json)` when finished.
 */
@Singleton
class JsSkillWebViewBridge @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val webViewHolder = AtomicReference<WebView?>(null)

    suspend fun execute(
        skillId: String,
        toolName: String,
        argumentsJson: String,
        allowNetwork: Boolean,
    ): String = suspendCancellableCoroutine { cont ->
        val safeArgs = argumentsJson.ifBlank { "{}" }
        val scriptBody = loadScript(skillId, toolName)
        val html = buildHtml(scriptBody, toolName, safeArgs, allowNetwork)
        mainHandler.post {
            val webView = obtainWebView()
            val bridge = object {
                @JavascriptInterface
                fun postToolResult(json: String) {
                    mainHandler.post {
                        if (cont.isActive) cont.resume(json)
                    }
                }
            }
            webView.removeJavascriptInterface("NibbliSkill")
            webView.addJavascriptInterface(bridge, "NibbliSkill")
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(
                        "if (typeof runTool === 'function') { runTool('$toolName', $safeArgs); } " +
                            "else { NibbliSkill.postToolResult(" +
                            "'{\"ok\":true,\"tool\":\"$toolName\",\"note\":\"inline skill\"}'); }",
                        null,
                    )
                }
            }
            webView.loadDataWithBaseURL(
                if (allowNetwork) "https://local.nibbli/" else null,
                html,
                "text/html",
                "UTF-8",
                null,
            )
        }
        cont.invokeOnCancellation { }
    }

    private fun loadScript(skillId: String, toolName: String): String {
        val fromFiles = File(context.filesDir, "skills/$skillId/scripts/$toolName.js")
        if (fromFiles.exists()) return fromFiles.readText()
        return """
            function runTool(name, args) {
              NibbliSkill.postToolResult(JSON.stringify({ ok: true, tool: name, args: args }));
            }
        """.trimIndent()
    }

    private fun buildHtml(script: String, toolName: String, args: String, allowNetwork: Boolean): String {
        val csp = if (allowNetwork) {
            "default-src 'self' https: data:; script-src 'unsafe-inline' 'unsafe-eval';"
        } else {
            "default-src 'none'; script-src 'unsafe-inline' 'unsafe-eval';"
        }
        return """
            <!DOCTYPE html><html><head>
            <meta http-equiv="Content-Security-Policy" content="$csp">
            </head><body><script>$script</script></body></html>
        """.trimIndent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun obtainWebView(): WebView {
        webViewHolder.get()?.let { return it }
        val latch = CountDownLatch(1)
        val holder = AtomicReference<WebView>()
        mainHandler.post {
            val existing = webViewHolder.get()
            if (existing != null) {
                holder.set(existing)
            } else {
                val webView = WebView(context.applicationContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                }
                webViewHolder.set(webView)
                holder.set(webView)
            }
            latch.countDown()
        }
        latch.await(5, TimeUnit.SECONDS)
        return holder.get() ?: webViewHolder.get()
            ?: error("WebView not initialized")
    }
}
