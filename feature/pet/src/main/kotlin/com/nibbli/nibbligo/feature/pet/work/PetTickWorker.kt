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
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetEngagementRules
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.feature.pet.domain.PetEngagementEngine
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetActions
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetSnapshot
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class PetTickWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val petRepository: PetRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
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
        PetWidgetSnapshot.write(applicationContext, state)
        PetWidgetUpdater.refresh(applicationContext)

        if (state.condition == PetCondition.DEAD) {
            return Result.success()
        }

        if (!userPreferencesRepository.getPetNotificationsEnabled()) {
            return Result.success()
        }

        val petName = state.name.ifBlank { "nibbli" }
        when {
            tick.shouldNotifyAttention -> notifyAttention(petName, state.activeNeed.name)
            PetEngagementEngine.isStreakAtRisk(state, now) ->
                notifyStreakAtRisk(petName, state.engagement.careStreakDays)
            shouldNotifyQuestReminder(state, now) -> notifyQuestReminder(petName, state)
        }
        return Result.success()
    }

    private fun shouldNotifyQuestReminder(state: com.nibbli.nibbligo.core.model.PetState, nowMillis: Long): Boolean {
        if (PetEngagementRules.dailyQuestComplete(state.engagement)) return false
        val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
        return hour >= 18
    }

    private fun notifyQuestReminder(petName: String, state: com.nibbli.nibbligo.core.model.PetState) {
        val remaining = listOfNotNull(
            "feed".takeIf { !state.engagement.dailyQuestFeed },
            "play".takeIf { !state.engagement.dailyQuestPlay },
            "talk".takeIf { !state.engagement.dailyQuestTalk },
        ).joinToString(", ")
        postNotification(
            channelId = CHANNEL_QUEST,
            channelName = "Daily quest",
            notificationId = QUEST_NOTIFICATION_ID,
            title = "$petName's daily quest",
            text = "Still to do today: $remaining",
            includeFeedAction = false,
        )
    }

    private fun notifyStreakAtRisk(petName: String, streakDays: Int) {
        postNotification(
            channelId = CHANNEL_STREAK,
            channelName = "Care streak",
            notificationId = STREAK_NOTIFICATION_ID,
            title = "$petName misses you",
            text = "Your $streakDays-day care streak needs a check-in today.",
            includeFeedAction = false,
        )
    }

    private fun notifyAttention(petName: String, needLabel: String) {
        postNotification(
            channelId = CHANNEL_NEEDS,
            channelName = "Pet needs",
            notificationId = NOTIFICATION_ID,
            title = "$petName is ${needLabel.lowercase()}",
            text = needText(needLabel),
            includeFeedAction = needLabel == PetNeed.HUNGRY.name,
        )
    }

    private fun postNotification(
        channelId: String,
        channelName: String,
        notificationId: Int,
        title: String,
        text: String,
        includeFeedAction: Boolean,
    ) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT),
        )
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_myplaces)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppPendingIntent())
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open Home",
                openAppPendingIntent(requestCode = OPEN_APP_REQUEST_CODE),
            )
        if (includeFeedAction) {
            builder.addAction(
                android.R.drawable.ic_input_add,
                "Quick feed",
                feedPendingIntent(),
            )
        }
        nm.notify(notificationId, builder.build())
    }

    private fun openAppPendingIntent(requestCode: Int = OPEN_APP_REQUEST_CODE): PendingIntent {
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
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun feedPendingIntent(): PendingIntent {
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(PetWidgetActions.EXTRA, PetWidgetActions.FEED)
            }
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(applicationContext.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(PetWidgetActions.EXTRA, PetWidgetActions.FEED)
            }
        return PendingIntent.getActivity(
            applicationContext,
            FEED_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun needText(needLabel: String): String = when (needLabel) {
        PetNeed.HUNGRY.name -> "Open nibbliGO to feed your pet."
        PetNeed.SICK.name -> "Medicine time — open nibbliGO."
        PetNeed.DIRTY.name -> "Time to clean up!"
        PetNeed.TIRED.name -> "Your pet is very tired."
        PetNeed.UNHAPPY.name -> "Your pet could use some attention."
        else -> "Check on your pet in the app."
    }

    companion object {
        const val WORK_TAG = "pet_tick"
        private const val CHANNEL_NEEDS = "pet_needs"
        private const val CHANNEL_STREAK = "pet_streak"
        private const val CHANNEL_QUEST = "pet_quest"
        private const val NOTIFICATION_ID = 42
        private const val STREAK_NOTIFICATION_ID = 43
        private const val QUEST_NOTIFICATION_ID = 44
        private const val OPEN_APP_REQUEST_CODE = 1001
        private const val FEED_REQUEST_CODE = 1002
    }
}
