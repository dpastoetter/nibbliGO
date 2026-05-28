package com.nibbli.nibbligo.core.domain.event

import com.nibbli.nibbligo.core.model.PetEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<PetEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<PetEvent> = _events.asSharedFlow()

    fun emit(event: PetEvent) {
        _events.tryEmit(event)
    }
}
