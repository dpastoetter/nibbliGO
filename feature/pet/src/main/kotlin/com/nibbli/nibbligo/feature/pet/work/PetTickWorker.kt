package com.nibbli.nibbligo.feature.pet.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.feature.pet.domain.PetEngagementEngine
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PetTickWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val petRepository: PetRepository,
) : CoroutineWorker(context, params) {

    private val engine = PetSimulationEngine()

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        var state = petRepository.getPetState()
        val tick = engine.tick(state, now)
        state = tick.state
        tick.templateDialogue?.let { line ->
            state = state.copy(dialogueLine = line)
        }
        petRepository.savePetState(state)

        if (tick.shouldNotifyAttention && state.condition != PetCondition.DEAD) {
            notifyAttention(state.activeNeed.name)
        } else if (PetEngagementEngine.isStreakAtRisk(state, now) &&
            state.condition != PetCondition.DEAD
        ) {
            notifyStreakAtRisk(state.engagement.careStreakDays)
        }
        return Result.success()
    }

    private fun notifyStreakAtRisk(streakDays: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Pet care", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Streak at risk!")
            .setContentText("Your $streakDays-day care streak needs a check-in today.")
            .setContentIntent(openAppPendingIntent())
            .setAutoCancel(true)
            .build()
        nm.notify(STREAK_NOTIFICATION_ID, notification)
    }

    private fun notifyAttention(needLabel: String) {
        val channelId = CHANNEL_ID
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Pet care", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("nibbli needs you")
            .setContentText(needText(needLabel))
            .setContentIntent(openAppPendingIntent())
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun openAppPendingIntent(): PendingIntent {
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(applicationContext.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        return PendingIntent.getActivity(
            applicationContext,
            OPEN_APP_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun needText(needLabel: String): String = when (needLabel) {
        PetNeed.HUNGRY.name -> "Your pet is hungry — open nibbliGO to feed."
        PetNeed.SICK.name -> "nibbli is sick — medicine time!"
        PetNeed.DIRTY.name -> "Time to clean up!"
        PetNeed.TIRED.name -> "nibbli is very tired."
        else -> "Check on nibbli in the app."
    }

    companion object {
        const val WORK_TAG = "pet_tick"
        private const val CHANNEL_ID = "pet_care"
        private const val NOTIFICATION_ID = 42
        private const val STREAK_NOTIFICATION_ID = 43
        private const val OPEN_APP_REQUEST_CODE = 1001
    }
}
