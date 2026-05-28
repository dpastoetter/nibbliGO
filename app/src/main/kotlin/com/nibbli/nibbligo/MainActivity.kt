package com.nibbli.nibbligo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.nibbli.nibbligo.core.designsystem.theme.NibbliTheme
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthHandler
import com.nibbli.nibbligo.navigation.NibbliApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var huggingFaceAuthHandler: HuggingFaceAuthHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NibbliTheme {
                NibbliApp()
            }
        }
        handleHuggingFaceRedirect(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleHuggingFaceRedirect(intent)
    }

    private fun handleHuggingFaceRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "nibbli" || data.host != "oauth") return
        lifecycleScope.launch {
            huggingFaceAuthHandler.handleAuthorizationResponse(intent)
        }
    }
}
