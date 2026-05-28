package com.nibbli.nibbligo.core.storage.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nibbli.nibbligo.core.domain.download.HfModelDownloadScheduler
import com.nibbli.nibbligo.core.storage.work.ModelDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HfModelDownloadSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : HfModelDownloadScheduler {
    override fun enqueueLiteRtModelDownload(modelId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(ModelDownloadWorker.input(modelId))
            .setConstraints(constraints)
            .addTag(ModelDownloadWorker.WORK_TAG)
            .addTag("hf_download_$modelId")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
