package com.nibbli.nibbligo.feature.pet.widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.nibbli.nibbligo.core.domain.pet.PetDeepLinkBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Non-exported trampoline for home-screen widget / notification pet actions.
 * Keeps [com.nibbli.nibbligo.MainActivity] from accepting forgeable widget extras from other apps.
 */
@AndroidEntryPoint
class PetWidgetTrampolineActivity : ComponentActivity() {

    @Inject lateinit var petDeepLinkBus: PetDeepLinkBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent?.getStringExtra(PetWidgetActions.EXTRA)
        if (action == PetWidgetActions.FEED || action == PetWidgetActions.TALK) {
            petDeepLinkBus.submitWidgetAction(action)
        }
        startActivity(
            Intent().apply {
                setClassName(packageName, "com.nibbli.nibbligo.MainActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
        )
        finish()
    }

    companion object {
        fun intent(context: Context, action: String): Intent =
            Intent(context, PetWidgetTrampolineActivity::class.java).apply {
                putExtra(PetWidgetActions.EXTRA, action)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
    }
}
