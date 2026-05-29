package com.nibbli.nibbligo.core.hf.download

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Downloads Hugging Face resolve URLs (302 → CDN). Optional bearer token for gated repos.
 * Supports resume via partial `.download` temp files and HTTP Range requests.
 */
object HfFileDownloader {
    private const val BUFFER_SIZE = 256 * 1024

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

        val resumeOffset = if (tmp.isFile && tmp.length() > 0L) tmp.length() else 0L

        val requestBuilder = Request.Builder().url(url).get()
        if (!accessToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }
        if (resumeOffset > 0L) {
            requestBuilder.header("Range", "bytes=$resumeOffset-")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            when (response.code) {
                200 -> {
                    if (resumeOffset > 0L && tmp.exists()) {
                        tmp.delete()
                    }
                    if (!response.isSuccessful) {
                        throw httpError(response.code, response.message)
                    }
                    writeBody(
                        tmp = tmp,
                        append = false,
                        startOffset = 0L,
                        totalBytes = response.body?.contentLength()?.coerceAtLeast(-1L) ?: -1L,
                        body = response.body ?: error("empty response body"),
                        onProgress = onProgress,
                    )
                }
                206 -> {
                    val totalBytes = parseContentRangeTotal(response.header("Content-Range"))
                        ?: (resumeOffset + (response.body?.contentLength()?.coerceAtLeast(0L) ?: 0L))
                    writeBody(
                        tmp = tmp,
                        append = true,
                        startOffset = resumeOffset,
                        totalBytes = totalBytes,
                        body = response.body ?: error("empty response body"),
                        onProgress = onProgress,
                    )
                }
                416 -> {
                    if (resumeOffset > 0L && tmp.length() > 0L) {
                        onProgress?.invoke(tmp.length(), tmp.length())
                    } else {
                        throw httpError(response.code, response.message)
                    }
                }
                else -> {
                    if (!response.isSuccessful) {
                        throw httpError(response.code, response.message)
                    }
                    writeBody(
                        tmp = tmp,
                        append = resumeOffset > 0L,
                        startOffset = if (resumeOffset > 0L) resumeOffset else 0L,
                        totalBytes = response.body?.contentLength()?.coerceAtLeast(-1L) ?: -1L,
                        body = response.body ?: error("empty response body"),
                        onProgress = onProgress,
                    )
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

    private fun writeBody(
        tmp: File,
        append: Boolean,
        startOffset: Long,
        totalBytes: Long,
        body: okhttp3.ResponseBody,
        onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?,
    ) {
        var sessionBytes = 0L
        body.byteStream().use { input ->
            FileOutputStream(tmp, append).use { fileOut ->
                BufferedOutputStream(fileOut, BUFFER_SIZE).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        sessionBytes += read
                        onProgress?.invoke(startOffset + sessionBytes, totalBytes)
                    }
                }
            }
        }
    }

    private fun parseContentRangeTotal(contentRange: String?): Long? {
        if (contentRange.isNullOrBlank()) return null
        val totalPart = contentRange.substringAfter('/', missingDelimiterValue = "")
        if (totalPart.isBlank() || totalPart == "*") return null
        return totalPart.toLongOrNull()
    }

    private fun httpError(code: Int, message: String): HfDownloadException =
        HfDownloadException(
            httpCode = code,
            message = when (code) {
                401 -> "Hugging Face authorization failed. Sign in or paste an access token in Settings."
                403 -> "Access denied. Accept the model license on huggingface.co and use a token with gated-repos scope."
                else -> "HTTP $code: $message"
            },
        )
}

class HfDownloadException(
    val httpCode: Int,
    override val message: String,
) : Exception(message)
