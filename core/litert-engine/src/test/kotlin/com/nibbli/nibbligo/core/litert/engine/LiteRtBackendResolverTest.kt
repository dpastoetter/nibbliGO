package com.nibbli.nibbligo.core.litert.engine

import com.nibbli.nibbligo.core.model.LiteRtAccelerator
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import org.junit.Assert.assertEquals
import org.junit.Test

class LiteRtBackendResolverTest {

    @Test
    fun functionGemma_isCpuOnlyInAuto() {
        val order = LiteRtBackendResolver.resolveAcceleratorOrder(
            modelId = "functiongemma-270m",
            userPreference = LiteRtAcceleratorPreference.AUTO,
            allowNpuInAuto = true,
        )
        assertEquals(listOf(LiteRtAccelerator.CPU), order)
    }

    @Test
    fun smollm2_prefersGpuThenCpuInAuto() {
        val order = LiteRtBackendResolver.resolveAcceleratorOrder(
            modelId = "smollm2-360m-instruct",
            userPreference = LiteRtAcceleratorPreference.AUTO,
            allowNpuInAuto = false,
        )
        assertEquals(listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU), order)
    }

    @Test
    fun auto_onEmulator_skipsGpu() {
        if (!LiteRtBackendResolver.isProbablyEmulator()) return
        val order = LiteRtBackendResolver.resolveAcceleratorOrder(
            modelId = "smollm2-360m-instruct",
            userPreference = LiteRtAcceleratorPreference.AUTO,
        )
        assertEquals(listOf(LiteRtAccelerator.CPU), order)
    }

    @Test
    fun smollm2_prependsNpuOnPhysicalDeviceInAuto() {
        val order = LiteRtBackendResolver.resolveAcceleratorOrder(
            modelId = "smollm2-360m-instruct",
            userPreference = LiteRtAcceleratorPreference.AUTO,
            allowNpuInAuto = true,
        )
        assertEquals(
            listOf(LiteRtAccelerator.NPU, LiteRtAccelerator.GPU, LiteRtAccelerator.CPU),
            order,
        )
    }

    @Test
    fun userGpuOverride_triesGpuThenCpu() {
        val order = LiteRtBackendResolver.resolveAcceleratorOrder(
            modelId = "smollm2-360m-instruct",
            userPreference = LiteRtAcceleratorPreference.GPU,
            allowNpuInAuto = true,
        )
        assertEquals(listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU), order)
    }

    @Test
    fun userGpuOverride_respectsCpuOnlyModel() {
        val order = LiteRtBackendResolver.resolveAcceleratorOrder(
            modelId = "functiongemma-270m",
            userPreference = LiteRtAcceleratorPreference.GPU,
            allowNpuInAuto = true,
        )
        assertEquals(listOf(LiteRtAccelerator.CPU), order)
    }

    @Test
    fun userNpuOverride_triesNpuGpuCpuForChatModels() {
        val order = LiteRtBackendResolver.resolveAcceleratorOrder(
            modelId = "gemma3-1b-it",
            userPreference = LiteRtAcceleratorPreference.NPU,
            allowNpuInAuto = true,
        )
        assertEquals(
            listOf(LiteRtAccelerator.NPU, LiteRtAccelerator.GPU, LiteRtAccelerator.CPU),
            order,
        )
    }

    @Test
    fun userCpuOverride_isCpuOnly() {
        val order = LiteRtBackendResolver.resolveAcceleratorOrder(
            modelId = "smollm2-360m-instruct",
            userPreference = LiteRtAcceleratorPreference.CPU,
            allowNpuInAuto = true,
        )
        assertEquals(listOf(LiteRtAccelerator.CPU), order)
    }
}
