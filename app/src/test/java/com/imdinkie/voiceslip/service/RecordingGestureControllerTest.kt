package com.imdinkie.voiceslip.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingGestureControllerTest {
    @Test
    fun quickTapStartsConfirmedRecording() {
        val controller = RecordingGestureController(longPressMillis = 400, touchSlopPx = 4)

        controller.onDown(rawX = 100f, rawY = 200f, overlayX = 10, overlayY = 20, nowMillis = 0)
        val action = controller.onUp(rawX = 101f, rawY = 201f, nowMillis = 120)

        assertEquals(RecordingGestureAction.StartConfirmedRecording, action)
    }

    @Test
    fun movingBeforeLongPressDragsBubbleAndSuppressesTapRecording() {
        val controller = RecordingGestureController(longPressMillis = 400, touchSlopPx = 4)

        controller.onDown(rawX = 100f, rawY = 200f, overlayX = 10, overlayY = 20, nowMillis = 0)
        val move = controller.onMove(rawX = 120f, rawY = 230f, nowMillis = 100)
        val up = controller.onUp(rawX = 120f, rawY = 230f, nowMillis = 120)

        assertEquals(RecordingGestureAction.MoveBubble(x = 30, y = 50), move)
        assertEquals(RecordingGestureAction.None, up)
    }

    @Test
    fun longPressStartsPushToTalkAndReleaseSubmitsAfterMinimumDuration() {
        val controller = RecordingGestureController(longPressMillis = 400, touchSlopPx = 4)

        controller.onDown(rawX = 100f, rawY = 200f, overlayX = 10, overlayY = 20, nowMillis = 0)
        val hold = controller.onLongPressTimeout(nowMillis = 400)
        val drift = controller.onMove(rawX = 160f, rawY = 240f, nowMillis = 500)
        val up = controller.onUp(rawX = 160f, rawY = 240f, nowMillis = 1100)

        assertEquals(RecordingGestureAction.StartPushToTalk, hold)
        assertEquals(RecordingGestureAction.None, drift)
        assertEquals(RecordingGestureAction.SubmitPushToTalk, up)
    }

    @Test
    fun pushToTalkReleaseAlwaysSubmitsAfterLongPressStarts() {
        val controller = RecordingGestureController(longPressMillis = 400, touchSlopPx = 4)

        controller.onDown(rawX = 100f, rawY = 200f, overlayX = 10, overlayY = 20, nowMillis = 0)
        controller.onLongPressTimeout(nowMillis = 400)
        val up = controller.onUp(rawX = 100f, rawY = 200f, nowMillis = 450)

        assertEquals(RecordingGestureAction.SubmitPushToTalk, up)
    }
}
