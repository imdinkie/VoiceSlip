package com.example.voiceslip.service

import org.junit.Assert.assertEquals
import org.junit.Test

class BubblePlacementTest {
    @Test
    fun placementRestoresSameEdgeAndVerticalFractionAfterRotation() {
        val portrait = BubbleBounds(width = 1080, height = 2340, bubbleWidth = 72, bubbleHeight = 72, edgePadding = 12)
        val landscape = BubbleBounds(width = 2340, height = 1080, bubbleWidth = 72, bubbleHeight = 72, edgePadding = 12)

        val saved = BubblePlacement.fromPosition(x = 996, y = 1140, bounds = portrait)
        val restored = saved.toPosition(landscape)

        assertEquals(2256, restored.x)
        assertEquals(507, restored.y)
    }
}
