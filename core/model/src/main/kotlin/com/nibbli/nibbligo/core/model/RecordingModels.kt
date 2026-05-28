package com.nibbli.nibbligo.core.model

data class AudioRecording(
    val id: Long = 0,
    val uri: String,
    val durationMs: Long,
    val transcript: String?,
    val summary: String?,
    val createdAtMillis: Long,
)
