package com.nibbli.nibbligo.core.agent.tools

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AgentToolPreviewTest {
    @Test
    fun sendEmail_preview_includes_recipient_and_subject() {
        val preview = AgentToolPreview.description(
            "phone_send_email",
            """{"to":"alex@example.com","subject":"Lunch","body":"See you at noon"}""",
        )
        assertTrue(preview.contains("alex@example.com"))
        assertTrue(preview.contains("Lunch"))
    }

    @Test
    fun createCalendar_preview_includes_title_and_time() {
        val preview = AgentToolPreview.description(
            "phone_create_calendar",
            """{"title":"Team sync","datetime":"2026-05-31T15:00:00"}""",
        )
        assertTrue(preview.contains("Team sync"))
        assertTrue(preview.contains("2026-05-31T15:00:00"))
    }
}
