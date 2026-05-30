package com.nibbli.nibbligo.feature.models

import com.nibbli.nibbligo.core.model.LiteRtAccelerator
import com.nibbli.nibbligo.core.model.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun qwen_is_recommended_for_nibbligo() {
        assertEquals(ModelCatalog.RECOMMENDED_MODEL_ID, "qwen2.5-1.5b-instruct")
        val model = ModelCatalog.find("qwen2.5-1.5b-instruct")
        assertTrue(model?.recommendedForNibbliGo == true)
        assertEquals(1, ModelCatalog.models.count { it.recommendedForNibbliGo })
    }

    @Test
    fun catalog_contains_litert_models() {
        assertTrue(ModelCatalog.models.any { it.id == "functiongemma-270m" })
        assertTrue(ModelCatalog.models.any { it.id == "gemma-4-e2b-it" })
        assertTrue(ModelCatalog.models.any { it.id == "gemma3-1b-it" })
        assertTrue(ModelCatalog.models.any { it.id == "qwen2.5-1.5b-instruct" })
        assertTrue(ModelCatalog.models.any { it.id == "deepseek-r1-distill-qwen-1.5b" })
        assertTrue(ModelCatalog.models.any { it.id == "smollm2-360m-instruct" })
    }

    @Test
    fun litert_models_require_download() {
        assertTrue(ModelCatalog.models.all { it.requiresLiteRt })
    }

    @Test
    fun functiongemma_requires_hf_auth() {
        val model = ModelCatalog.find("functiongemma-270m")
        assertTrue(model?.requiresHfAuth == true)
    }

    @Test
    fun gemma3_requires_hf_auth() {
        val model = ModelCatalog.find("gemma3-1b-it")
        assertTrue(model?.requiresHfAuth == true)
    }

    @Test
    fun smollm2_does_not_require_hf_auth() {
        val model = ModelCatalog.find("smollm2-360m-instruct")
        assertTrue(model?.requiresHfAuth == false)
    }

    @Test
    fun displayName_falls_back_to_id() {
        assertEquals("Gemma 3 1B IT", ModelCatalog.displayName("gemma3-1b-it"))
        assertEquals("unknown-model", ModelCatalog.displayName("unknown-model"))
    }

    @Test
    fun functiongemma_isCpuOnlyAccelerator() {
        val model = ModelCatalog.find("functiongemma-270m")
        assertEquals(listOf(LiteRtAccelerator.CPU), model?.preferredAccelerators)
    }

    @Test
    fun chatModels_preferGpuThenCpu() {
        val chatModelIds = listOf(
            "smollm2-360m-instruct",
            "gemma3-1b-it",
            "gemma-4-e2b-it",
            "qwen2.5-1.5b-instruct",
            "deepseek-r1-distill-qwen-1.5b",
        )
        chatModelIds.forEach { id ->
            val model = ModelCatalog.find(id)
            assertEquals(
                "Expected GPU then CPU for $id",
                listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU),
                model?.preferredAccelerators,
            )
        }
    }

    @Test
    fun gemma4_hasGalleryAlignedContextAndAccelerators() {
        val model = ModelCatalog.find("gemma-4-e2b-it")
        assertEquals(4000, model?.maxContextTokens)
        assertEquals(
            listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU),
            model?.preferredAccelerators,
        )
    }
}
