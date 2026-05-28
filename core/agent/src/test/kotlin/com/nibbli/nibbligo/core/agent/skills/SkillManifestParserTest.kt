package com.nibbli.nibbligo.core.agent.skills

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillManifestParserTest {

    @Test
    fun parse_frontmatter_and_tools() {
        val md = """
            ---
            name: Test Skill
            description: A test
            version: 2.0.0
            permissions: local_storage, network
            ---
            # Body
            ### tool: hello
        """.trimIndent()
        val parsed = SkillManifestParser.parse("test_skill", md, hasJsRuntime = false)
        assertEquals("Test Skill", parsed.displayName)
        assertEquals(2, parsed.permissions.size)
        assertTrue(parsed.tools.any { it.name == "hello" })
    }

    @Test
    fun parse_defaults_run_tool_when_no_tool_blocks() {
        val parsed = SkillManifestParser.parse("x", "# Skill\nDoes things.", hasJsRuntime = true)
        assertEquals("run", parsed.tools.single().name)
        assertTrue(parsed.hasJsRuntime)
    }
}
