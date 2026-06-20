package com.nibbli.nibbligo

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nibbli.nibbligo.core.agent.skills.SkillPackageLoader
import com.nibbli.nibbligo.core.domain.model.InstalledModelPathResolver
import com.nibbli.nibbligo.core.domain.model.ModelRuntimeCoordinator
import com.nibbli.nibbligo.feature.pet.work.PetTickScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
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
    @Inject lateinit var installedModelPathResolver: InstalledModelPathResolver
    @Inject lateinit var modelRuntimeCoordinator: ModelRuntimeCoordinator

    private val appExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught exception in appScope", throwable)
    }

    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + appExceptionHandler,
    )

    override fun onCreate() {
        super.onCreate()
        petTickScheduler.schedule()
        appScope.launch {
            installedModelPathResolver.refreshCache()
        }
        appScope.launch {
            skillPackageLoader.loadBundledSkills()
        }
        appScope.launch {
            liteRtModelPreloader.preloadPrimaryModel()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            Log.i(TAG, "onTrimMemory level=$level — unloading LiteRT sessions")
            modelRuntimeCoordinator.unloadAll()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        private const val TAG = "NibbliApplication"
    }
}
