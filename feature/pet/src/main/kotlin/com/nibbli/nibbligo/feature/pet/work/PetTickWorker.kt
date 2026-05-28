package com.nibbli.nibbligo.feature.pet.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetNeed
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
        }
        return Result.success()
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
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
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
    }
}
