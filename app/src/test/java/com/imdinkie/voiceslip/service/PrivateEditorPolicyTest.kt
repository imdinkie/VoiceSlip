package com.imdinkie.voiceslip.service

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateEditorPolicyTest {
    @Test
    fun privateEditorDoesNotHideBubbleByItself() {
        assertTrue(shouldShowBubbleForField(secretField = false))
    }

    @Test
    fun secretFieldsAlwaysHideBubble() {
        assertFalse(shouldShowBubbleForField(secretField = true))
    }

    @Test
    fun privateImeFlagDoesNotBlockInsertion() {
        assertFalse(shouldBlockInsertionForField(secretAccessibilityNode = false, secretInputEditor = false))
    }

    @Test
    fun hintOnlyTextCanUseSetTextWithoutSelection() {
        assertTrue(shouldSetTextWithoutSelection(currentText = "Send chat", hintText = "Send chat"))
    }

    @Test
    fun realTextDoesNotGetReplacedWhenSelectionIsUnavailable() {
        assertFalse(shouldSetTextWithoutSelection(currentText = "Draft message", hintText = "Send chat"))
    }

    @Test
    fun pasteIsPreferredBeforeUnverifiedInputMethodCommit() {
        assertTrue(
            insertionAttemptOrder(
                hasInsertionTarget = true,
                supportsSetText = true,
                supportsPaste = true,
                canUseInputMethod = true
            ).let { it.indexOf(InsertionAttempt.PASTE) < it.indexOf(InsertionAttempt.COMMIT_TEXT) }
        )
    }

    @Test
    fun unverifiedInputMethodCommitRemainsBeforeCopyAsLastInsertionAttempt() {
        assertTrue(
            insertionAttemptOrder(
                hasInsertionTarget = false,
                supportsSetText = false,
                supportsPaste = false,
                canUseInputMethod = true
            ).let { it == listOf(InsertionAttempt.COMMIT_TEXT, InsertionAttempt.COPY) }
        )
    }

    @Test
    fun passwordInputTypeIsSecretButPrivateImeFlagIsNot() {
        assertTrue(isSecretInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertFalse(isSecretInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
    }
}
