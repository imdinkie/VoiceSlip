package com.example.voiceslip.service

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
    fun passwordInputTypeIsSecretButPrivateImeFlagIsNot() {
        assertTrue(isSecretInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertFalse(isSecretInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI))
    }
}
