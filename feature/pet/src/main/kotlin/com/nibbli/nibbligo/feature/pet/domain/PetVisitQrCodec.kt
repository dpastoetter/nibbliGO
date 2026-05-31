package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.PetState
import java.util.Base64

object PetVisitQrCodec {
    private const val PREFIX = "nibbli:visit:1:"
    private const val MAX_PAYLOAD_BYTES = 2_048

    fun encode(state: PetState): String {
        val json = PetPostcardCodec.encode(state)
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
            json.toByteArray(Charsets.UTF_8),
        )
        return PREFIX + encoded
    }

    fun decode(raw: String): PetPostcard? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        val payload = trimmed.removePrefix(PREFIX)
        if (payload.isEmpty() || payload.length > MAX_PAYLOAD_BYTES * 2) return null
        val jsonBytes = runCatching {
            Base64.getUrlDecoder().decode(payload)
        }.getOrNull() ?: return null
        if (jsonBytes.size > MAX_PAYLOAD_BYTES) return null
        val json = jsonBytes.toString(Charsets.UTF_8)
        return PetPostcardCodec.decode(json)
    }
}
