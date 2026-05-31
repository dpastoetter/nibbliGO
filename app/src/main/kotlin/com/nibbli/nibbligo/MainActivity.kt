package com.nibbli.nibbligo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nibbli.nibbligo.core.domain.pet.PetDeepLinkBus
import com.nibbli.nibbligo.core.hf.download.HuggingFaceAuthHandler
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetActions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var huggingFaceAuthHandler: HuggingFaceAuthHandler
    @Inject lateinit var petDeepLinkBus: PetDeepLinkBus

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            NibbliAppWithTheme()
        }
        handleWidgetAction(intent)
        handleDeepLink(intent)
        handleHuggingFaceRedirect(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetAction(intent)
        handleDeepLink(intent)
        handleHuggingFaceRedirect(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun handleWidgetAction(intent: Intent?) {
        intent?.getStringExtra(PetWidgetActions.EXTRA)?.let { action ->
            petDeepLinkBus.submitWidgetAction(action)
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "nibbli" || data.host != "challenge") return
        if (data.path != "/catch") return
        val score = data.getQueryParameter("score")?.toIntOrNull() ?: return
        petDeepLinkBus.submitCatchChallenge(score)
    }

    private fun handleHuggingFaceRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "nibbli" || data.host != "oauth") return
        lifecycleScope.launch {
            huggingFaceAuthHandler.handleAuthorizationResponse(intent)
        }
    }
}
