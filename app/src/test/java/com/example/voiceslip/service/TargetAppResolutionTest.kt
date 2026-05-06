package com.example.voiceslip.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetAppResolutionTest {
    @Test
    fun targetAppUsesCurrentHighConfidenceSignalsInOrder() {
        assertEquals(
            "com.focused.window",
            resolveTargetAppPackage(
                focusedWindowPackage = "com.focused.window",
                activeRootPackage = "com.active.root",
                editableNodePackage = "com.editor.node",
                inputEditorPackage = "com.input.editor",
                ownPackage = "com.example.voiceslip"
            )
        )

        assertEquals(
            "com.editor.node",
            resolveTargetAppPackage(
                focusedWindowPackage = null,
                activeRootPackage = null,
                editableNodePackage = "com.editor.node",
                inputEditorPackage = "com.input.editor",
                ownPackage = "com.example.voiceslip"
            )
        )
    }

    @Test
    fun targetAppDoesNotUseVoiceSlipOrBlankPackages() {
        assertNull(
            resolveTargetAppPackage(
                focusedWindowPackage = "com.example.voiceslip",
                activeRootPackage = "",
                editableNodePackage = null,
                inputEditorPackage = null,
                ownPackage = "com.example.voiceslip"
            )
        )
    }
}
