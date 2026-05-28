package com.nibbli.nibbligo.feature.models

import com.nibbli.nibbligo.core.model.Modality
import com.nibbli.nibbligo.core.model.ModelCatalog
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun catalog_contains_demo_models() {
        assertTrue(ModelCatalog.models.any { it.id == "nibbli-fast" })
        assertTrue(ModelCatalog.models.any { it.id == "nibbli-vision" })
    }

    @Test
    fun vision_model_supports_vision_modality() {
        val vision = ModelCatalog.find("nibbli-vision")
        assertNotNull(vision)
        assertTrue(vision!!.modalities.contains(Modality.VISION))
    }
}
