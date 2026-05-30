package com.nibbli.nibbligo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nibbli.nibbligo.core.agent.skills.SkillPackageLoader
import com.nibbli.nibbligo.feature.pet.work.PetTickScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NibbliApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var skillPackageLoader: SkillPackageLoader
    @Inject lateinit var petTickScheduler: PetTickScheduler
    @Inject lateinit var liteRtModelPreloader: com.nibbli.nibbligo.core.pet.llm.LiteRtModelPreloader

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        petTickScheduler.schedule()
        appScope.launch {
            skillPackageLoader.loadBundledSkills()
        }
        appScope.launch {
            liteRtModelPreloader.preloadPrimaryModel()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
