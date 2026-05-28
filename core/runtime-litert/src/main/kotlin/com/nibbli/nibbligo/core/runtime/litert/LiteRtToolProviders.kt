package com.nibbli.nibbligo.core.runtime.litert

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.nibbli.nibbligo.core.model.ToolRiskLevel

/**
 * Gallery-style mobile actions ToolSet for FunctionGemma / tool-capable models.
 * Ported from gallery@main: MobileActionsTools.kt (Apache 2.0)
 */
class NibbliMobileActionsToolSet(
    private val onAction: (String) -> Unit,
) : ToolSet {

    @Tool(description = "Turns the flashlight on")
    fun turnOnFlashlight(): Map<String, String> {
        onAction("flashlight_on")
        return mapOf("result" to "success")
    }

    @Tool(description = "Turns the flashlight off")
    fun turnOffFlashlight(): Map<String, String> {
        onAction("flashlight_off")
        return mapOf("result" to "success")
    }

    @Tool(description = "Opens Android system settings")
    fun openSettings(
        @ToolParam(description = "Settings screen id") screen: String = "main",
    ): Map<String, String> {
        onAction("open_settings:$screen")
        return mapOf("result" to "success", "screen" to screen)
    }
}

fun toolProvidersForModel(
    modelId: String,
    @Suppress("UNUSED_PARAMETER") onMobileAction: (String) -> Unit,
): List<com.google.ai.edge.litertlm.ToolProvider> {
    // ToolSet types are registered at the agent layer; LiteRT engine runs chat without native ToolSet binding here.
    return emptyList()
}

fun ToolRiskLevel.requiresConfirmation(): Boolean = this == ToolRiskLevel.SENSITIVE
