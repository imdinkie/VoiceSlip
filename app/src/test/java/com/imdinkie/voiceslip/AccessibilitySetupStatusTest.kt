package com.imdinkie.voiceslip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilitySetupStatusTest {
    @Test
    fun serviceIsEnabledWhenFlattenedComponentMatchesPackageAndClass() {
        assertTrue(
            isVoiceSlipAccessibilityServiceEnabled(
                packageName = "com.imdinkie.voiceslip",
                enabledServiceIds = listOf("com.imdinkie.voiceslip/com.imdinkie.voiceslip.service.VoiceSlipAccessibilityService"),
                serviceConnected = false
            )
        )
    }

    @Test
    fun serviceIsEnabledWhenClassNameIsRelative() {
        assertTrue(
            isVoiceSlipAccessibilityServiceEnabled(
                packageName = "com.imdinkie.voiceslip",
                enabledServiceIds = listOf("com.imdinkie.voiceslip/.service.VoiceSlipAccessibilityService"),
                serviceConnected = false
            )
        )
    }

    @Test
    fun serviceIsEnabledWhenLiveServiceIsConnected() {
        assertTrue(
            isVoiceSlipAccessibilityServiceEnabled(
                packageName = "com.imdinkie.voiceslip",
                enabledServiceIds = emptyList(),
                serviceConnected = true
            )
        )
    }

    @Test
    fun unrelatedServiceDoesNotEnableVoiceSlip() {
        assertFalse(
            isVoiceSlipAccessibilityServiceEnabled(
                packageName = "com.imdinkie.voiceslip",
                enabledServiceIds = listOf("com.other/.OtherService"),
                serviceConnected = false
            )
        )
    }
}
