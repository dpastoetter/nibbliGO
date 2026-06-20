package com.nibbli.nibbligo.core.runtime.litert

import com.nibbli.nibbligo.core.domain.model.ModelRuntimeCoordinator
import com.nibbli.nibbligo.core.litert.engine.LiteRtEnginePool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtModelRuntimeCoordinator @Inject constructor(
    private val enginePool: LiteRtEnginePool,
) : ModelRuntimeCoordinator {
    override fun unloadModel(modelId: String) {
        enginePool.unload(modelId)
    }

    override fun unloadAll() {
        enginePool.unloadAll()
    }
}
