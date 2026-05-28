// Ported from gallery@main: GalleryWebView.kt + WebViewAssetLoader (Apache 2.0)
package com.nibbli.nibbligo.core.agent.skills

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
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
        val webView = WebView(context.applicationContext)
        val skillDir = File(context.filesDir, "skills/$skillId")
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .addPathHandler("/", WebViewAssetLoader.InternalStoragePathHandler(context, context.filesDir))
            .build()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(
            object {
                @JavascriptInterface
                fun postToolResult(json: String) {
                    if (cont.isActive) cont.resume(json)
                }
            },
            "GallerySkill",
        )
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ) = request?.url?.let { assetLoader.shouldInterceptRequest(it) }

            override fun onPageFinished(view: WebView?, url: String?) {
                val script = """
                  if (typeof runTool === 'function') {
                    runTool('$toolName', $argumentsJson);
                  } else if (typeof GallerySkill !== 'undefined') {
                    GallerySkill.postToolResult('{"ok":true,"tool":"$toolName"}');
                  }
                """.trimIndent()
                view?.evaluateJavascript(script, null)
            }
        }
        val indexHtml = File(skillDir, "scripts/index.html")
        val url = if (indexHtml.exists()) {
            "$GALLERY_LOCAL_URL_BASE/skills/$skillId/scripts/index.html"
        } else {
            val inline = File(skillDir, "scripts/$toolName.js")
            if (inline.exists()) {
                null
            } else {
                null
            }
        }
        if (url != null) {
            webView.loadUrl(url)
        } else {
            val js = File(skillDir, "scripts/$toolName.js").takeIf { it.exists() }?.readText()
                ?: defaultRunToolJs()
            val html = """<html><body><script>$js</script></body></html>"""
            webView.loadDataWithBaseURL(
                if (allowNetwork) "$GALLERY_LOCAL_URL_BASE/" else null,
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
}
