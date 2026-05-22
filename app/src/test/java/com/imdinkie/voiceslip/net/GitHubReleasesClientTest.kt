package com.imdinkie.voiceslip.net

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleasesClientTest {
    @Test
    fun newerPatchReleaseIsDetected() {
        assertTrue(isReleaseNewer("1.0.0", "v1.0.1"))
    }

    @Test
    fun equalReleaseIsNotNewer() {
        assertFalse(isReleaseNewer("1.0.0", "v1.0.0"))
    }

    @Test
    fun debugSuffixIsIgnored() {
        assertFalse(isReleaseNewer("1.0.0-debug", "v1.0.0"))
    }

    @Test
    fun buildMetadataIsIgnored() {
        assertFalse(isReleaseNewer("1.0.0+local", "v1.0.0"))
    }

    @Test
    fun shorterInstalledVersionIsPadded() {
        assertTrue(isReleaseNewer("1.0", "v1.0.1"))
    }

    @Test
    fun malformedTagsAreIgnored() {
        assertFalse(isReleaseNewer("1.0.0", "release-one"))
    }
}
