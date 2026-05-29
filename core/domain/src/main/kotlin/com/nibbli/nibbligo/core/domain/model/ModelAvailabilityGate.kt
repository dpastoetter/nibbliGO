package com.nibbli.nibbligo.core.domain.model

interface ModelAvailabilityGate {
    suspend fun hasUsableModel(): Boolean
    suspend fun firstUsableModelId(): String?
}
