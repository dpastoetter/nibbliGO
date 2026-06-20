package com.nibbli.nibbligo.core.storage.model

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nibbli.nibbligo.core.storage.local.NibbliDatabase
import com.nibbli.nibbligo.core.storage.local.entity.ModelInstallEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class InstalledModelPathResolverImplTest {

    private lateinit var context: Context
    private lateinit var db: NibbliDatabase
    private lateinit var resolver: InstalledModelPathResolverImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NibbliDatabase::class.java).build()
        resolver = InstalledModelPathResolverImpl(context, db.modelInstallDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun refreshCache_readsInstalledPathWithoutBlocking() = runTest {
        val modelFile = File(context.filesDir, "models/test.litertlm").apply {
            parentFile?.mkdirs()
            writeText("x".repeat(2_000_000))
        }
        db.modelInstallDao().insert(
            ModelInstallEntity(
                modelId = "smollm2-360m-instruct",
                localPath = modelFile.absolutePath,
                installedAtMillis = 0L,
                sizeBytes = modelFile.length(),
            ),
        )
        resolver.refreshCache()
        val resolved = resolver.resolveFile("smollm2-360m-instruct")
        assertNotNull(resolved)
        assertEquals(modelFile.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun refreshCache_singleModel_removesMissingEntry() = runTest {
        resolver.refreshCache("missing-model")
        assertNull(resolver.resolveFile("missing-model"))
    }
}
