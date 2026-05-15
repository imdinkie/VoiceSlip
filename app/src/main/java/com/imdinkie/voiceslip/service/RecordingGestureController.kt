package com.imdinkie.voiceslip.service

import kotlin.math.abs

internal class RecordingGestureController(
    private val longPressMillis: Long = 400,
    private val touchSlopPx: Int = 4
) {
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var downAtMillis = 0L
    private var pushToTalkStartedAtMillis: Long? = null
    private var moved = false

    fun onDown(rawX: Float, rawY: Float, overlayX: Int, overlayY: Int, nowMillis: Long) {
        downRawX = rawX
        downRawY = rawY
        startX = overlayX
        startY = overlayY
        downAtMillis = nowMillis
        pushToTalkStartedAtMillis = null
        moved = false
    }

    fun onMove(rawX: Float, rawY: Float, nowMillis: Long): RecordingGestureAction {
        if (pushToTalkStartedAtMillis != null) return RecordingGestureAction.None
        val dx = rawX - downRawX
        val dy = rawY - downRawY
        if (abs(dx) > touchSlopPx || abs(dy) > touchSlopPx) moved = true
        return RecordingGestureAction.MoveBubble(startX + dx.toInt(), startY + dy.toInt())
    }

    fun onLongPressTimeout(nowMillis: Long): RecordingGestureAction {
        if (moved || nowMillis - downAtMillis < longPressMillis) return RecordingGestureAction.None
        pushToTalkStartedAtMillis = nowMillis
        return RecordingGestureAction.StartPushToTalk
    }

    fun onUp(rawX: Float, rawY: Float, nowMillis: Long): RecordingGestureAction {
        if (pushToTalkStartedAtMillis != null) return RecordingGestureAction.SubmitPushToTalk
        if (moved) return RecordingGestureAction.None
        return RecordingGestureAction.StartConfirmedRecording
    }
}

internal sealed interface RecordingGestureAction {
    data object None : RecordingGestureAction
    data object StartConfirmedRecording : RecordingGestureAction
    data object StartPushToTalk : RecordingGestureAction
    data object SubmitPushToTalk : RecordingGestureAction
    data class MoveBubble(val x: Int, val y: Int) : RecordingGestureAction
}
