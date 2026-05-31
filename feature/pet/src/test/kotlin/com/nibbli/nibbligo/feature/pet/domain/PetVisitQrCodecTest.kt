package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PetVisitQrCodecTest {

    @Test
    fun encodeDecode_roundTrip() {
        val state = PetState(
            name = "Pixel",
            stage = LifeStage.CHILD,
            careScore = 42,
            dialogueLine = "Come visit me!",
            stats = PetStats(mood = 88),
        )
        val payload = PetVisitQrCodec.encode(state)
        assertTrue(payload.startsWith("nibbli:visit:1:"))
        val decoded = PetVisitQrCodec.decode(payload)
        assertNotNull(decoded)
        assertEquals("Pixel", decoded!!.senderName)
        assertEquals(LifeStage.CHILD, decoded.stage)
        assertEquals(42, decoded.careScore)
        assertEquals(88, decoded.mood)
    }

    @Test
    fun decode_rejects_unknown_prefix() {
        assertNull(PetVisitQrCodec.decode("nibbli:other:1:abc"))
        assertNull(PetVisitQrCodec.decode("not-a-qr-payload"))
    }
}
