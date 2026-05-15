package com.imdinkie.voiceslip.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguagePreservationPreferenceTest {
    @Test
    fun absentPreferenceDefaultsPreserveSpokenLanguageOn() {
        assertTrue(resolvePreserveSpokenLanguagePreference(null))
    }

    @Test
    fun explicitPreferenceIsPreserved() {
        assertTrue(resolvePreserveSpokenLanguagePreference(true))
        assertFalse(resolvePreserveSpokenLanguagePreference(false))
    }
}
