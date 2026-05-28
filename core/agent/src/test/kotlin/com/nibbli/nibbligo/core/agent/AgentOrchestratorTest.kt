package com.nibbli.nibbligo.core.agent

import com.nibbli.nibbligo.core.agent.execution.ToolExecutor
import com.nibbli.nibbligo.core.agent.skills.GallerySkillWebViewBridge
import com.nibbli.nibbligo.core.mcp.McpToolRegistry
import com.nibbli.nibbligo.core.agent.tools.ToolRegistry
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ActionHistoryRepository
import com.nibbli.nibbligo.core.domain.repository.SkillPackageRepository
import com.nibbli.nibbligo.core.model.AgentSessionState
import com.nibbli.nibbligo.core.runtime.fake.FakeInferenceRuntime
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AgentOrchestratorTest {

    private lateinit var orchestrator: AgentOrchestrator
    private val runtime = FakeInferenceRuntime()
    private val toolRegistry = ToolRegistry()
    private val actionHistory: ActionHistoryRepository = mockk(relaxed = true)
    private val skillPackages: SkillPackageRepository = mockk(relaxed = true)
    private val galleryBridge: GallerySkillWebViewBridge = mockk(relaxed = true)
    private val mcpRegistry: McpToolRegistry = mockk(relaxed = true)
    private val petBus = PetEventBus()

    @Before
    fun setup() {
        coEvery { actionHistory.log(any(), any(), any()) } returns Unit
        val executor = ToolExecutor(toolRegistry, actionHistory, skillPackages, galleryBridge, mcpRegistry)
        orchestrator = AgentOrchestrator(runtime, toolRegistry, executor, petBus)
    }

    @Test
    fun runTurn_reminder_proposes_tool_then_completes_after_confirm() = runTest {
        runtime.ensureModelLoaded("nibbli-fast")
        val session = AgentSessionState(modelId = "nibbli-fast")
        val first = orchestrator.runTurn(
            modelId = "nibbli-fast",
            userMessage = "remind me to stretch",
            session = session,
            autoApproveSafeTools = false,
        )
        assertNotNull(first.pendingConfirmation)
        val confirmed = orchestrator.confirmAndContinue(
            modelId = "nibbli-fast",
            session = first.session,
            pending = first.pendingConfirmation!!,
        )
        assertNull(confirmed.pendingConfirmation)
        assertNotNull(confirmed.finalText)
    }
}
