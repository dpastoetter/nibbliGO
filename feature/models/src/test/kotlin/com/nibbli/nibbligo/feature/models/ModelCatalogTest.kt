package com.nibbli.nibbligo.feature.models

import com.nibbli.nibbligo.core.model.ModelCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun catalog_contains_litert_models() {
        assertTrue(ModelCatalog.models.any { it.id == "functiongemma-270m" })
        assertTrue(ModelCatalog.models.any { it.id == "gemma-4-e2b-it" })
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
}
