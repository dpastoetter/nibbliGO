package com.nibbli.nibbligo.core.hf.download

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Downloads Hugging Face resolve URLs (302 → CDN). Optional bearer token for gated repos.
 */
object HfFileDownloader {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.MINUTES)
        .writeTimeout(15, TimeUnit.MINUTES)
        .build()

    fun download(
        url: String,
        destination: File,
        accessToken: String? = null,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null,
    ) {
        val tmp = File(destination.parentFile, "${destination.name}.download")
        tmp.parentFile?.mkdirs()
        if (tmp.exists()) tmp.delete()

        val requestBuilder = Request.Builder().url(url).get()
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw HfDownloadException(
                    httpCode = response.code,
                    message = when (response.code) {
                        401 -> "Hugging Face authorization failed. Sign in or paste an access token in Settings."
                        403 -> "Access denied. Accept the model license on huggingface.co and use a token with gated-repos scope."
                        else -> "HTTP ${response.code}: ${response.message}"
                    },
                )
            }
            val body = response.body ?: error("empty response body")
            val totalBytes = body.contentLength().coerceAtLeast(-1L)
            var bytesRead = 0L
            body.byteStream().use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesRead += read
                        onProgress?.invoke(bytesRead, totalBytes)
                    }
                }
            }
        }
        if (destination.exists()) destination.delete()
        if (!tmp.renameTo(destination)) {
            tmp.inputStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            tmp.delete()
        }
    }
}

class HfDownloadException(
    val httpCode: Int,
    override val message: String,
) : Exception(message)
