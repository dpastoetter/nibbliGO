package com.nibbli.nibbligo.core.litert.engine

import android.util.Log

/** Structured timing logs for pet inference tuning (enable via `adb shell setprop log.tag.LiteRtPetTiming DEBUG`). */
internal object LiteRtPetTiming {
    private const val TAG = "LiteRtPetTiming"

    fun log(stage: String, elapsedMs: Long) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "$stage=${elapsedMs}ms")
        }
    }
}
