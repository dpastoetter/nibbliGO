package com.nibbli.nibbligo.core.domain.model

/** Notifies on-device inference runtimes when installed models change. */
interface ModelRuntimeCoordinator {
    fun unloadModel(modelId: String)

    fun unloadAll() = Unit
}
