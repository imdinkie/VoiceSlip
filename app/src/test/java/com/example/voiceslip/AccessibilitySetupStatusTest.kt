package com.example.voiceslip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilitySetupStatusTest {
    @Test
    fun serviceIsEnabledWhenFlattenedComponentMatchesPackageAndClass() {
        assertTrue(
            isVoiceSlipAccessibilityServiceEnabled(
                packageName = "com.example.voiceslip",
                enabledServiceIds = listOf("com.example.voiceslip/com.example.voiceslip.service.VoiceSlipAccessibilityService"),
                serviceConnected = false
            )
        )
    }

    @Test
    fun serviceIsEnabledWhenClassNameIsRelative() {
        assertTrue(
            isVoiceSlipAccessibilityServiceEnabled(
                packageName = "com.example.voiceslip",
                enabledServiceIds = listOf("com.example.voiceslip/.service.VoiceSlipAccessibilityService"),
                serviceConnected = false
            )
        )
    }

    @Test
    fun serviceIsEnabledWhenLiveServiceIsConnected() {
        assertTrue(
            isVoiceSlipAccessibilityServiceEnabled(
                packageName = "com.example.voiceslip",
                enabledServiceIds = emptyList(),
                serviceConnected = true
            )
        )
    }

    @Test
    fun unrelatedServiceDoesNotEnableVoiceSlip() {
        assertFalse(
            isVoiceSlipAccessibilityServiceEnabled(
                packageName = "com.example.voiceslip",
                enabledServiceIds = listOf("com.other/.OtherService"),
                serviceConnected = false
            )
        )
    }
}
