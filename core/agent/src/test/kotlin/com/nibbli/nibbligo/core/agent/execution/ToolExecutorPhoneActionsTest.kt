package com.nibbli.nibbligo.core.agent.execution

import com.nibbli.nibbligo.core.agent.skills.GallerySkillWebViewBridge
import com.nibbli.nibbligo.core.agent.tools.ToolRegistry
import com.nibbli.nibbligo.core.domain.repository.ActionHistoryRepository
import com.nibbli.nibbligo.core.domain.repository.SkillPackageRepository
import com.nibbli.nibbligo.core.mcp.McpToolRegistry
import com.nibbli.nibbligo.core.mobileactions.MobileActionsPerformer
import com.nibbli.nibbligo.core.model.ToolCall
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ToolExecutorPhoneActionsTest {

    private val toolRegistry = ToolRegistry()
    private val actionHistory: ActionHistoryRepository = mockk(relaxed = true)
    private val skillPackages: SkillPackageRepository = mockk(relaxed = true)
    private val galleryBridge: GallerySkillWebViewBridge = mockk(relaxed = true)
    private val mcpRegistry: McpToolRegistry = mockk(relaxed = true)
    private val mobileActions: MobileActionsPerformer = mockk(relaxed = true)
    private lateinit var executor: ToolExecutor

    @Before
    fun setup() {
        executor = ToolExecutor(
            toolRegistry,
            actionHistory,
            skillPackages,
            galleryBridge,
            mcpRegistry,
            mobileActions,
        )
    }

    @Test
    fun sendEmail_executesViaMobileActionsPerformer() = runTest {
        every { mobileActions.sendEmail(any()) } returns ""

        val result = executor.execute(
            ToolCall(
                toolId = "phone_send_email",
                argumentsJson = """{"to":"a@b.com","subject":"Hi","body":"Hello"}""",
            ),
        )

        assertTrue(result.success)
        verify { mobileActions.sendEmail(any()) }
    }

    @Test
    fun sendEmail_propagatesPerformerError() = runTest {
        every { mobileActions.sendEmail(any()) } returns "No email app"

        val result = executor.execute(
            ToolCall(
                toolId = "sendEmail",
                argumentsJson = """{"to":"a@b.com","subject":"Hi","body":"Hello"}""",
            ),
        )

        assertFalse(result.success)
        assertTrue(result.outputJson.contains("No email app"))
    }
}
