package com.example.voiceslip.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.voiceslip.audio.VoiceRecorder
import com.example.voiceslip.data.HistoryItem
import com.example.voiceslip.data.RecordingStatus
import com.example.voiceslip.data.SecretStore
import com.example.voiceslip.data.VoiceSlipRepository
import com.example.voiceslip.net.MistralTranscriptionClient
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class VoiceSlipAccessibilityService : AccessibilityService() {
    private lateinit var windowManager: WindowManager
    private lateinit var repository: VoiceSlipRepository
    private lateinit var secretStore: SecretStore
    private lateinit var recorder: VoiceRecorder
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlay: RecordingOverlay? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var currentItem: HistoryItem? = null
    private var recordingStartedAt = 0L
    private var overlayExpanded = false
    private var compactAnchorX = -1
    private var compactAnchorY = -1

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WindowManager::class.java)
        repository = VoiceSlipRepository(this)
        secretStore = SecretStore(this)
        recorder = VoiceRecorder(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        refreshOverlayVisibility()
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder.cancel()
        hideOverlay()
        if (instance === this) instance = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (overlay != null) updateOverlaySize(overlayExpanded)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName == packageName) {
            if (activeApplicationPackage() == packageName) {
                scheduleOverlayRefresh(delayMillis = 80)
            }
            return
        }
        scheduleOverlayRefresh(delayMillis = 80)
    }

    override fun onInterrupt() {
        recorder.cancel()
        hideOverlay()
    }

    private val refreshRunnable = Runnable { refreshOverlayVisibility() }
    private val showRunnable = Runnable {
        if (!recorder.isRecording && shouldShowBubble()) showOverlay(expanded = false)
    }
    private val hideRunnable = Runnable {
        if (!recorder.isRecording && !shouldShowBubble()) hideOverlay()
    }

    private fun scheduleOverlayRefresh(delayMillis: Long) {
        mainHandler.removeCallbacks(refreshRunnable)
        mainHandler.postDelayed(refreshRunnable, delayMillis)
    }

    private fun refreshOverlayVisibility() {
        if (recorder.isRecording) return
        val shouldShow = shouldShowBubble()
        if (shouldShow) {
            mainHandler.removeCallbacks(hideRunnable)
            mainHandler.removeCallbacks(showRunnable)
            mainHandler.postDelayed(showRunnable, SHOW_DEBOUNCE_MS)
        } else {
            mainHandler.removeCallbacks(showRunnable)
            mainHandler.removeCallbacks(hideRunnable)
            mainHandler.postDelayed(hideRunnable, HIDE_DEBOUNCE_MS)
        }
    }

    private fun shouldShowBubble(): Boolean {
        if (!hasInputMethodWindow()) return false
        val activePackage = activeApplicationPackage()
        if (activePackage == packageName) return false
        val node = findFocusedEditableNode()
        if (node != null && isSensitiveNode(node)) return false
        return true
    }

    private fun hasInputMethodWindow(): Boolean {
        return windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
    }

    private fun activeApplicationPackage(): String? {
        return windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }?.root?.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
    }

    private fun findFocusedEditableNode(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { isEditableNode(it) }
            ?: findEditableNode(root)
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && isEditableNode(node)) return node
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val found = findEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun isEditableNode(node: AccessibilityNodeInfo): Boolean {
        if (!node.isEnabled) return false
        if (node.isEditable) return true
        return node.actionList.any {
            it.id == AccessibilityNodeInfo.ACTION_SET_TEXT || it.id == AccessibilityNodeInfo.ACTION_PASTE
        }
    }

    private fun isSensitiveNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        val text = "${node.text?.toString().orEmpty()} ${node.hintText?.toString().orEmpty()} ${node.contentDescription?.toString().orEmpty()}".lowercase()
        val sensitiveTerms = listOf("password", "passcode", "pin", "otp", "one-time", "credit card", "card number", "cvv", "cvc")
        return sensitiveTerms.any { it in text }
    }

    private fun showOverlay(expanded: Boolean) {
        mainHandler.removeCallbacks(showRunnable)
        mainHandler.removeCallbacks(hideRunnable)
        overlayExpanded = expanded
        val existing = overlay
        if (existing != null) {
            updateOverlaySize(expanded)
            return
        }

        val compactSize = compactSizePx()
        val view = RecordingOverlay(
            context = this,
            compactSizePx = compactSize,
            onBubbleClick = { startRecording() },
            onCancel = { cancelRecording() },
            onSubmit = { submitRecording() },
            onMove = { x, y -> moveOverlay(x, y) }
        )
        view.setExpanded(expanded)
        overlay = view

        val width = if (expanded) expandedWidthPx(compactSize) else compactSize
        val height = compactSize
        val savedX = repository.getBubbleX()
        val savedY = repository.getBubbleY()
        val defaultPosition = clampBubblePosition(
            resources.displayMetrics.widthPixels - compactSize - edgePaddingPx(),
            resources.displayMetrics.heightPixels / 3,
            compactSize,
            compactSize
        )
        val compactPosition = if (savedX >= 0 && savedY >= 0) {
            clampBubblePosition(savedX, savedY, compactSize, compactSize)
        } else {
            defaultPosition
        }
        compactAnchorX = compactPosition.first
        compactAnchorY = compactPosition.second
        val position = if (expanded) positionForExpandedOverlay(compactPosition.first, compactPosition.second, width, height) else compactPosition
        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = position.first
            y = position.second
        }
        if (!expanded) repository.setBubblePosition(params.x, params.y)
        overlayParams = params
        runCatching { windowManager.addView(view, params) }
    }

    private fun updateOverlaySize(expanded: Boolean) {
        val view = overlay ?: return
        val params = overlayParams ?: return
        val compactSize = compactSizePx()
        view.setCompactSize(compactSize)
        view.setExpanded(expanded)
        val width = if (expanded) expandedWidthPx(compactSize) else compactSize
        val height = compactSize
        val position = if (expanded) {
            val anchorX = compactAnchorX.takeIf { it >= 0 } ?: params.x
            val anchorY = compactAnchorY.takeIf { it >= 0 } ?: params.y
            positionForExpandedOverlay(anchorX, anchorY, width, height)
        } else {
            val anchorX = compactAnchorX.takeIf { it >= 0 } ?: params.x
            val anchorY = compactAnchorY.takeIf { it >= 0 } ?: params.y
            clampBubblePosition(anchorX, anchorY, width, height)
        }
        if (
            params.width == width &&
            params.height == height &&
            params.x == position.first &&
            params.y == position.second &&
            overlayExpanded == expanded
        ) {
            return
        }
        overlayExpanded = expanded
        params.width = width
        params.height = height
        params.x = position.first
        params.y = position.second
        if (!expanded) {
            compactAnchorX = params.x
            compactAnchorY = params.y
            repository.setBubblePosition(params.x, params.y)
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun hideOverlay() {
        mainHandler.removeCallbacks(showRunnable)
        mainHandler.removeCallbacks(hideRunnable)
        val view = overlay ?: return
        overlay = null
        overlayParams = null
        overlayExpanded = false
        compactAnchorX = -1
        compactAnchorY = -1
        runCatching { windowManager.removeView(view) }
    }

    private fun moveOverlay(rawX: Int, rawY: Int) {
        val view = overlay ?: return
        val params = overlayParams ?: return
        val clamped = clampBubblePosition(rawX, rawY, params.width, params.height)
        if (params.x == clamped.first && params.y == clamped.second) return
        params.x = clamped.first
        params.y = clamped.second
        if (!overlayExpanded) {
            compactAnchorX = clamped.first
            compactAnchorY = clamped.second
            repository.setBubblePosition(clamped.first, clamped.second)
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun compactSizePx(): Int = dp(repository.getBubbleSize().dp)

    private fun expandedWidthPx(compactSize: Int): Int = (compactSize * 3.6f).toInt()

    private fun edgePaddingPx(): Int = dp(12)

    private fun positionForExpandedOverlay(compactX: Int, compactY: Int, expandedWidth: Int, expandedHeight: Int): Pair<Int, Int> {
        val compactSize = compactSizePx()
        val bounds = screenBounds()
        val edgePadding = edgePaddingPx()
        val opensLeft = compactX + compactSize / 2 > bounds.width() / 2
        val desiredX = if (opensLeft) compactX + compactSize - expandedWidth else compactX
        return clampBubblePosition(desiredX, compactY, expandedWidth, expandedHeight)
    }

    private fun clampBubblePosition(rawX: Int, rawY: Int, width: Int, height: Int): Pair<Int, Int> {
        val bounds = screenBounds()
        val edgePadding = edgePaddingPx()
        val maxX = max(edgePadding, bounds.width() - width - edgePadding)
        val maxY = max(edgePadding, bounds.height() - height - edgePadding)
        return rawX.coerceIn(edgePadding, maxX) to rawY.coerceIn(edgePadding, maxY)
    }

    private fun screenBounds(): Rect {
        return if (android.os.Build.VERSION.SDK_INT >= 30) {
            windowManager.currentWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            Rect().also { windowManager.defaultDisplay.getRectSize(it) }
        }
    }

    private fun startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            toast("Microphone permission is required")
            return
        }
        if (secretStore.getMistralApiKey().isNullOrBlank()) {
            toast("Add your Mistral API key in VoiceSlip")
            return
        }
        val id = UUID.randomUUID().toString()
        val file = File(repository.recordingsDir, "$id.m4a")
        runCatching {
            recorder.start(file)
            haptic()
            recordingStartedAt = System.currentTimeMillis()
            currentItem = HistoryItem(
                id = id,
                createdAtMillis = recordingStartedAt,
                audioPath = file.absolutePath,
                durationMillis = 0L,
                status = RecordingStatus.RECORDING
            ).also { repository.upsertHistory(it) }
            showOverlay(expanded = true)
            overlay?.setRecordingState(RecordingUiState.RECORDING)
            mainHandler.post(tickRunnable)
        }.onFailure {
            toast("Could not start recording: ${it.message}")
        }
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!recorder.isRecording) return
            val elapsed = System.currentTimeMillis() - recordingStartedAt
            val remaining = MAX_RECORDING_MS - elapsed
            overlay?.updateAmplitude(recorder.maxAmplitude(), remaining)
            if (remaining <= 0L) {
                submitRecording()
            } else {
                mainHandler.postDelayed(this, 120)
            }
        }
    }

    private fun cancelRecording() {
        mainHandler.removeCallbacks(tickRunnable)
        haptic()
        recorder.cancel()
        currentItem?.let {
            repository.upsertHistory(it.copy(status = RecordingStatus.CANCELED, durationMillis = System.currentTimeMillis() - it.createdAtMillis))
        }
        currentItem = null
        refreshOverlayVisibility()
    }

    private fun submitRecording() {
        if (!recorder.isRecording) return
        mainHandler.removeCallbacks(tickRunnable)
        haptic()
        val result = recorder.stop() ?: return
        val item = currentItem?.copy(
            durationMillis = result.durationMillis,
            status = RecordingStatus.TRANSCRIBING
        ) ?: HistoryItem(
            id = UUID.randomUUID().toString(),
            createdAtMillis = System.currentTimeMillis(),
            audioPath = result.file.absolutePath,
            durationMillis = result.durationMillis,
            status = RecordingStatus.TRANSCRIBING
        )
        currentItem = item
        repository.upsertHistory(item)
        overlay?.setRecordingState(RecordingUiState.TRANSCRIBING)
        Thread {
            val updated = runCatching {
                val apiKey = secretStore.getMistralApiKey().orEmpty()
                val dictionary = repository.listDictionary().map { it.phrase }
                val transcription = MistralTranscriptionClient().transcribe(apiKey, result.file, dictionary)
                val text = transcription.text
                val inserted = mainHandler.postAndWait { insertOrCopy(text) }
                item.copy(
                    status = RecordingStatus.SUCCEEDED,
                    transcript = text,
                    error = if (inserted) null else "Copied to clipboard because no editable field was available.",
                    model = transcription.model
                )
            }.getOrElse { error ->
                item.copy(
                    status = RecordingStatus.FAILED,
                    error = error.message ?: error::class.java.simpleName
                )
            }
            repository.upsertHistory(updated)
            mainHandler.post {
                currentItem = null
                if (updated.status == RecordingStatus.SUCCEEDED) {
                    toast(if (updated.error == null) "Inserted transcription" else "Copied transcription")
                } else {
                    toast("Transcription failed. Open VoiceSlip to retry.")
                }
                refreshOverlayVisibility()
            }
        }.start()
    }

    private fun insertOrCopy(text: String): Boolean {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("VoiceSlip transcription", text))
        val node = findFocusedEditableNode()
        if (node == null || isSensitiveNode(node)) return false
        if (node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_PASTE }) {
            return node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
        return false
    }

    private fun haptic() {
        if (!repository.getHapticsEnabled()) return
        val vibrator = getSystemService(Vibrator::class.java)
        if (!vibrator.hasVibrator()) return
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(18)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val MAX_RECORDING_MS = 5 * 60 * 1000L
        private const val SHOW_DEBOUNCE_MS = 250L
        private const val HIDE_DEBOUNCE_MS = 500L
        var instance: VoiceSlipAccessibilityService? = null
            private set
    }
}

private fun Handler.postAndWait(block: () -> Boolean): Boolean {
    if (Looper.myLooper() == Looper.getMainLooper()) return block()
    val lock = Object()
    var complete = false
    var result = false
    post {
        result = block()
        synchronized(lock) {
            complete = true
            lock.notifyAll()
        }
    }
    synchronized(lock) {
        while (!complete) lock.wait(5000)
    }
    return result
}

private enum class RecordingUiState {
    IDLE,
    RECORDING,
    TRANSCRIBING
}

private class BubbleIconView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(28, 24, 32)
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        paint.strokeWidth = size * 0.07f

        val micTop = centerY - size * 0.22f
        val micBottom = centerY + size * 0.12f
        val micRadius = size * 0.13f
        canvas.drawRoundRect(
            centerX - micRadius,
            micTop,
            centerX + micRadius,
            micBottom,
            micRadius,
            micRadius,
            paint
        )
        canvas.drawLine(centerX, micBottom + size * 0.03f, centerX, centerY + size * 0.28f, paint)
        canvas.drawLine(centerX - size * 0.14f, centerY + size * 0.28f, centerX + size * 0.14f, centerY + size * 0.28f, paint)

        val waveTop = centerY - size * 0.06f
        val waveBottom = centerY + size * 0.06f
        canvas.drawLine(centerX - size * 0.32f, waveTop, centerX - size * 0.32f, waveBottom, paint)
        canvas.drawLine(centerX + size * 0.32f, waveTop, centerX + size * 0.32f, waveBottom, paint)
        canvas.drawLine(centerX - size * 0.42f, centerY - size * 0.02f, centerX - size * 0.42f, centerY + size * 0.02f, paint)
        canvas.drawLine(centerX + size * 0.42f, centerY - size * 0.02f, centerX + size * 0.42f, centerY + size * 0.02f, paint)
    }
}

private class RecordingOverlay(
    context: Context,
    compactSizePx: Int,
    private val onBubbleClick: () -> Unit,
    private val onCancel: () -> Unit,
    private val onSubmit: () -> Unit,
    private val onMove: (Int, Int) -> Unit
) : FrameLayout(context) {
    private val collapsed = BubbleIconView(context)
    private val expanded = LinearLayout(context)
    private val waveform = WaveformView(context)
    private var compactSize = compactSizePx
    private val cancel = circle("×", Color.rgb(183, 178, 190), Color.rgb(28, 24, 32))
    private val submit = circle("✓", Color.rgb(88, 37, 115), Color.WHITE)
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var moved = false
    private var isExpanded = false
    private var uiState = RecordingUiState.IDLE

    init {
        setPadding(dp(4), dp(4), dp(4), dp(4))
        collapsed.background = bubbleBg(Color.rgb(194, 184, 205), dp(22).toFloat())
        addView(collapsed, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        expanded.orientation = LinearLayout.HORIZONTAL
        expanded.gravity = Gravity.CENTER
        expanded.visibility = GONE
        addView(expanded, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        waveform.background = bubbleBg(Color.rgb(194, 184, 205), dp(24).toFloat())
        expanded.addView(cancel)
        expanded.addView(waveform)
        expanded.addView(submit)
        updateExpandedChildSizes()

        cancel.setOnClickListener { onCancel() }
        submit.setOnClickListener { onSubmit() }

        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = (layoutParams as? WindowManager.LayoutParams)?.x ?: 0
                    startY = (layoutParams as? WindowManager.LayoutParams)?.y ?: 0
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) moved = true
                    onMove(startX + dx.toInt(), startY + dy.toInt())
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved && !isExpanded) onBubbleClick()
                    true
                }
                else -> false
            }
        }
    }

    fun setExpanded(expand: Boolean) {
        isExpanded = expand
        collapsed.visibility = if (expand) GONE else VISIBLE
        expanded.visibility = if (expand) VISIBLE else GONE
    }

    fun setCompactSize(sizePx: Int) {
        if (compactSize == sizePx) return
        compactSize = sizePx
        updateExpandedChildSizes()
        collapsed.invalidate()
        waveform.invalidate()
    }

    fun setRecordingState(state: RecordingUiState) {
        uiState = state
        waveform.setTranscribing(state == RecordingUiState.TRANSCRIBING)
    }

    fun updateAmplitude(amplitude: Int, remainingMillis: Long) {
        val countdown = if (remainingMillis <= 30_000L) max(0, (remainingMillis / 1000L).toInt()) else null
        waveform.setAmplitude(amplitude, countdown)
    }

    private fun circle(text: String, bgColor: Int, fgColor: Int): TextView = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_PX, compactSize * 0.32f)
        setTextColor(fgColor)
        gravity = Gravity.CENTER
        background = bubbleBg(bgColor, dp(35).toFloat())
    }

    private fun updateExpandedChildSizes() {
        val control = (compactSize * 0.78f).toInt()
        val gap = (compactSize * 0.09f).toInt()
        cancel.setTextSize(TypedValue.COMPLEX_UNIT_PX, compactSize * 0.32f)
        submit.setTextSize(TypedValue.COMPLEX_UNIT_PX, compactSize * 0.32f)
        cancel.layoutParams = LinearLayout.LayoutParams(control, control)
        waveform.layoutParams = LinearLayout.LayoutParams(0, control, 1f).apply {
            marginStart = gap
            marginEnd = gap
        }
        submit.layoutParams = LinearLayout.LayoutParams(control, control)
    }

    private fun bubbleBg(color: Int, radius: Float) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private class WaveformView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(28, 24, 32)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 7f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        textSize = 15f * resources.displayMetrics.density
    }
    private var level = 0.1f
    private var countdown: Int? = null
    private var transcribing = false

    fun setAmplitude(amplitude: Int, countdownSeconds: Int?) {
        level = min(1f, max(0.08f, amplitude / 32767f))
        countdown = countdownSeconds
        invalidate()
    }

    fun setTranscribing(value: Boolean) {
        transcribing = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val seconds = countdown
        if (transcribing) {
            canvas.drawText("transcribing", width / 2f, height / 2f + paint.textSize / 3f, paint)
            return
        }
        if (seconds != null) {
            canvas.drawText("${seconds}s", width / 2f, height / 2f + paint.textSize / 3f, paint)
            return
        }
        val centerY = height / 2f
        val bars = 9
        val gap = width / (bars + 1f)
        for (i in 1..bars) {
            val phase = if (i % 2 == 0) 0.55f else 1f
            val barHeight = (height * 0.16f) + (height * 0.55f * level * phase)
            val x = gap * i
            canvas.drawLine(x, centerY - barHeight / 2f, x, centerY + barHeight / 2f, paint)
        }
    }
}
