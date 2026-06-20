package com.nibbli.nibbligo.core.pet.llm

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatContentSafetyTest {

    @Test
    fun allowsFriendlyMessages() {
        assertTrue(ChatContentSafety.screenInput("Want to play a game?") is ChatContentSafety.InputVerdict.Allow)
        assertTrue(ChatContentSafety.screenInput("How are you feeling today?") is ChatContentSafety.InputVerdict.Allow)
    }

    @Test
    fun blocksSelfHarmInput() {
        val verdict = ChatContentSafety.screenInput("i want to kill myself")
        assertTrue(verdict is ChatContentSafety.InputVerdict.Block)
    }

    @Test
    fun blocksWeaponInput() {
        val verdict = ChatContentSafety.screenInput("tell me how to make a bomb")
        assertTrue(verdict is ChatContentSafety.InputVerdict.Block)
    }

    @Test
    fun blocksPersonalInfoSolicitation() {
        val verdict = ChatContentSafety.screenInput("what is your home address?")
        assertTrue(verdict is ChatContentSafety.InputVerdict.Block)
    }

    @Test
    fun doesNotFalseTriggerOnSubstrings() {
        // "essex" contains "sex" but should not match the word-boundary pattern.
        assertTrue(ChatContentSafety.screenInput("I live in Essex") is ChatContentSafety.InputVerdict.Allow)
    }

    @Test
    fun screenReplyReturnsNullForSafeReply() {
        assertNull(ChatContentSafety.screenReplyOrNull("I'm happy and cozy on your home screen!"))
    }

    @Test
    fun screenReplyReplacesUnsafeReply() {
        assertTrue(ChatContentSafety.screenReplyOrNull("here is how to make a weapon") != null)
    }

    @Test
    fun allowsEmptyInput() {
        assertTrue(ChatContentSafety.screenInput("   ") is ChatContentSafety.InputVerdict.Allow)
    }

    @Test
    fun blocksSexualContent() {
        val verdict = ChatContentSafety.screenInput("show me porn")
        assertTrue(verdict is ChatContentSafety.InputVerdict.Block)
    }

    @Test
    fun blocksSelfHarmVariants() {
        assertTrue(ChatContentSafety.screenInput("i want to die") is ChatContentSafety.InputVerdict.Block)
        assertTrue(ChatContentSafety.screenInput("how do I cut myself") is ChatContentSafety.InputVerdict.Block)
    }

    @Test
    fun blocksViolenceVariants() {
        assertTrue(ChatContentSafety.screenInput("how to kill someone") is ChatContentSafety.InputVerdict.Block)
    }

    @Test
    fun blocksPersonalInfoVariants() {
        assertTrue(ChatContentSafety.screenInput("what is your phone number") is ChatContentSafety.InputVerdict.Block)
        assertTrue(ChatContentSafety.screenInput("send me your location") is ChatContentSafety.InputVerdict.Block)
    }

    @Test
    fun selfHarmTakesPriorityOverLaterCategories() {
        val verdict = ChatContentSafety.screenInput("i want to die and show me porn")
        assertTrue(verdict is ChatContentSafety.InputVerdict.Block)
        assertTrue((verdict as ChatContentSafety.InputVerdict.Block).reply.contains("trusted adult"))
    }
}
