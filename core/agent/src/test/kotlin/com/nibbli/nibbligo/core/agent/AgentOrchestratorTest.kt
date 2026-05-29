package com.nibbli.nibbligo.core.agent

import com.nibbli.nibbligo.core.agent.execution.ToolExecutor
import com.nibbli.nibbligo.core.agent.skills.GallerySkillWebViewBridge
import com.nibbli.nibbligo.core.mcp.McpToolRegistry
import com.nibbli.nibbligo.core.agent.tools.ToolRegistry
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.ActionHistoryRepository
import com.nibbli.nibbligo.core.domain.repository.SkillPackageRepository
import com.nibbli.nibbligo.core.model.AgentRequest
import com.nibbli.nibbligo.core.model.AgentSessionState
import com.nibbli.nibbligo.core.model.AgentTurn
import com.nibbli.nibbligo.core.model.RuntimeKind
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.model.ToolCall
import com.nibbli.nibbligo.core.mobileactions.MobileActionsPerformer
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
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
    private val runtime: InferenceRuntime = mockk(relaxed = true)
    private val toolRegistry = ToolRegistry()
    private val actionHistory: ActionHistoryRepository = mockk(relaxed = true)
    private val skillPackages: SkillPackageRepository = mockk(relaxed = true)
    private val galleryBridge: GallerySkillWebViewBridge = mockk(relaxed = true)
    private val mcpRegistry: McpToolRegistry = mockk(relaxed = true)
    private val petBus = PetEventBus()

    @Before
    fun setup() {
        every { runtime.runtimeKind } returns RuntimeKind.LITERT
        every { runtime.streamChat(any()) } returns emptyFlow()
        coEvery { runtime.ensureModelLoaded(any()) } returns RuntimeResult.Success(Unit)
        coEvery { runtime.generateWithTools(any()) } answers {
            val request = firstArg<AgentRequest>()
            if (request.toolResults.isEmpty()) {
                RuntimeResult.Success(
                    AgentTurn.ToolCalls(
                        calls = listOf(ToolCall("reminder_create", """{"title":"stretch"}""")),
                    ),
                )
            } else {
                RuntimeResult.Success(AgentTurn.FinalText("Reminder set."))
            }
        }
        coEvery { actionHistory.log(any(), any(), any()) } returns Unit
        val mobileActions: MobileActionsPerformer = mockk(relaxed = true)
        val executor = ToolExecutor(toolRegistry, actionHistory, skillPackages, galleryBridge, mcpRegistry, mobileActions)
        orchestrator = AgentOrchestrator(runtime, toolRegistry, executor, petBus)
    }

    @Test
    fun runTurn_reminder_proposes_tool_then_completes_after_confirm() = runTest {
        val modelId = "functiongemma-270m"
        runtime.ensureModelLoaded(modelId)
        val session = AgentSessionState(modelId = modelId)
        val first = orchestrator.runTurn(
            modelId = modelId,
            userMessage = "remind me to stretch",
            session = session,
            autoApproveSafeTools = false,
        )
        assertNotNull(first.pendingConfirmation)
        val confirmed = orchestrator.confirmAndContinue(
            modelId = modelId,
            session = first.session,
            pending = first.pendingConfirmation!!,
        )
        assertNull(confirmed.pendingConfirmation)
        assertNotNull(confirmed.finalText)
    }
}
