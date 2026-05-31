package com.nibbli.nibbligo.feature.pet.domain

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.share.PetShareCard
import com.nibbli.nibbligo.feature.pet.ui.share.PetShareCardKind
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PetShareExporterTest {

    @Test
    fun renderCard_producesNonBlankBitmapOnActivity() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var bitmap: Bitmap? = null
        var error: Throwable? = null
        val worker = Thread {
            try {
                runBlocking {
                    bitmap = PetShareExporter.renderCard(activity, 360, 640) {
                        PetShareCard(kind = PetShareCardKind.TODAY, pet = PetState(name = "nibbli"))
                    }
                }
            } catch (t: Throwable) {
                error = t
            }
        }
        worker.start()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        while (worker.isAlive && System.nanoTime() < deadline) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(16)
        }
        worker.join(500)
        error?.let { throw it }
        requireNotNull(bitmap) { "Share card bitmap was not rendered" }
        assertEquals(360, bitmap!!.width)
        assertEquals(640, bitmap!!.height)
        assertTrue(hasVisibleContent(bitmap!!))
    }

    private fun hasVisibleContent(bitmap: Bitmap): Boolean {
        for (x in 0 until bitmap.width step 24) {
            for (y in 0 until bitmap.height step 24) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = pixel ushr 24
                if (alpha > 0 && pixel and 0x00FFFFFF != 0) return true
            }
        }
        return false
    }
}
