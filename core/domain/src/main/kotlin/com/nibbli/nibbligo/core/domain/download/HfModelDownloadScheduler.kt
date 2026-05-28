package com.nibbli.nibbligo.core.domain.download

interface HfModelDownloadScheduler {
    fun enqueueLiteRtModelDownload(modelId: String)
}
