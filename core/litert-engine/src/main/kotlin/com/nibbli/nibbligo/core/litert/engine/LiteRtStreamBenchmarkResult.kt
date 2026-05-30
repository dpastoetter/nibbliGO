package com.nibbli.nibbligo.core.litert.engine

data class LiteRtStreamBenchmarkResult(
    val timeToFirstTokenMs: Long,
    val totalMs: Long,
    val approximateTokenCount: Int,
    val tokensPerSecond: Float,
)
