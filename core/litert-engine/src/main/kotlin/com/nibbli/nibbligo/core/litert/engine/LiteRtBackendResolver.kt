package com.nibbli.nibbligo.core.litert.engine

import android.os.Build
import com.google.ai.edge.litertlm.Backend
import com.nibbli.nibbligo.core.model.LiteRtAccelerator
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import com.nibbli.nibbligo.core.model.ModelCatalog

object LiteRtBackendResolver {

    fun resolveAcceleratorOrder(
        modelId: String,
        userPreference: LiteRtAcceleratorPreference,
        allowNpuInAuto: Boolean = !isProbablyEmulator(),
    ): List<LiteRtAccelerator> {
        val modelDefault = ModelCatalog.find(modelId)?.preferredAccelerators
            ?: listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU)
        val cpuOnly = modelDefault == listOf(LiteRtAccelerator.CPU)

        val ordered = when (userPreference) {
            LiteRtAcceleratorPreference.AUTO -> when {
                isProbablyEmulator() -> listOf(LiteRtAccelerator.CPU)
                allowNpuInAuto && !cpuOnly && LiteRtAccelerator.NPU !in modelDefault -> {
                    listOf(LiteRtAccelerator.NPU) + modelDefault
                }
                else -> modelDefault
            }
            LiteRtAcceleratorPreference.GPU -> {
                if (cpuOnly) listOf(LiteRtAccelerator.CPU)
                else listOf(LiteRtAccelerator.GPU, LiteRtAccelerator.CPU)
            }
            LiteRtAcceleratorPreference.CPU -> listOf(LiteRtAccelerator.CPU)
            LiteRtAcceleratorPreference.NPU -> {
                if (cpuOnly) listOf(LiteRtAccelerator.CPU)
                else listOf(LiteRtAccelerator.NPU, LiteRtAccelerator.GPU, LiteRtAccelerator.CPU)
            }
        }
        return ordered.distinct()
    }

    fun resolveBackendOrder(
        modelId: String,
        userPreference: LiteRtAcceleratorPreference,
        nativeLibraryDir: String,
        allowNpuInAuto: Boolean = !isProbablyEmulator(),
    ): List<Backend> =
        resolveAcceleratorOrder(modelId, userPreference, allowNpuInAuto).map { accelerator ->
            toBackend(accelerator, nativeLibraryDir)
        }

    fun toBackend(accelerator: LiteRtAccelerator, nativeLibraryDir: String): Backend =
        when (accelerator) {
            LiteRtAccelerator.GPU -> Backend.GPU()
            LiteRtAccelerator.CPU -> Backend.CPU()
            LiteRtAccelerator.NPU -> Backend.NPU(nativeLibraryDir = nativeLibraryDir)
        }

    fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT?.lowercase() ?: return false
        val model = Build.MODEL?.lowercase() ?: ""
        val hardware = Build.HARDWARE?.lowercase() ?: ""
        return fingerprint.contains("generic") ||
            fingerprint.contains("emulator") ||
            model.contains("sdk") ||
            model.contains("emulator") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu")
    }
}
