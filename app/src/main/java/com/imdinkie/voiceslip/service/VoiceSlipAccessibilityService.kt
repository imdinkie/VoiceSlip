package com.imdinkie.voiceslip.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.InputMethod
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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.TextAttribute
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.imdinkie.voiceslip.audio.VoiceRecorder
import com.imdinkie.voiceslip.data.HistoryItem
import com.imdinkie.voiceslip.data.PipelineConfig
import com.imdinkie.voiceslip.data.PipelineMode
import com.imdinkie.voiceslip.data.RecordingStatus
import com.imdinkie.voiceslip.data.SecretStore
import com.imdinkie.voiceslip.data.StyleResolution
import com.imdinkie.voiceslip.data.VoiceSlipRepository
import com.imdinkie.voiceslip.net.PipelineException
import com.imdinkie.voiceslip.net.PipelineExecutor
import com.imdinkie.voiceslip.net.PipelineResult
import com.imdinkie.voiceslip.net.outputGuardRejection
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
    private var currentConfigSnapshot: PipelineConfig? = null
    private var currentStyleSnapshot: StyleResolution? = null
    private var recordingStartedAt = 0L
    private var currentInteraction = RecordingInteraction.CONFIRMED
    private var overlayExpanded = false
    private var overlayPushToTalk = false
    private var compactAnchorX = -1
    private var compactAnchorY = -1
    private var accessibilityInputMethod: VoiceSlipInputMethod? = null

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

    override fun onCreateInputMethod(): InputMethod {
        return VoiceSlipInputMethod(this).also { accessibilityInputMethod = it }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (overlay != null) updateOverlaySize(overlayExpanded)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName == packageName) {
            if (activeApplicationPackage() == packageName) {
                refreshOverlayVisibility()
            }
            return
        }
        repository.noteRecentApp(event?.packageName?.toString())
        refreshOverlayVisibility()
    }

    override fun onInterrupt() {
        recorder.cancel()
        hideOverlay()
    }

    fun refreshFromSettings() {
        refreshOverlayVisibility()
    }

    private fun refreshOverlayVisibility() {
        if (recorder.isRecording || currentItem?.status == RecordingStatus.TRANSCRIBING) return
        if (shouldShowBubble()) showCompactOverlayWithoutCollapseFlicker() else hideOverlay()
    }

    private fun showCompactOverlayWithoutCollapseFlicker() {
        if (overlayExpanded) hideOverlay()
        showOverlay(expanded = false)
    }

    private fun shouldShowBubble(): Boolean {
        if (!repository.getAppEnabled()) return false
        if (!hasInputMethodWindow()) return false
        val activePackage = activeApplicationPackage()
        if (activePackage == packageName) return false
        val node = findFocusedEditableNode()
        val secretField = (node != null && isSensitiveNode(node)) || accessibilityInputMethod?.isSensitiveEditor() == true
        return shouldShowBubbleForField(secretField = secretField)
    }

    private fun hasInputMethodWindow(): Boolean {
        return windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
    }

    private fun activeApplicationPackage(): String? {
        val focusedWindowPackage = windows.firstOrNull {
            it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused
        }?.root?.packageName?.toString()
        val activeRootPackage = rootInActiveWindow?.packageName?.toString()
        val editableNodePackage = findFocusedEditableNode()?.packageName?.toString()
        val inputEditorPackage = accessibilityInputMethod?.targetPackageName()
        return resolveTargetAppPackage(
            focusedWindowPackage = focusedWindowPackage,
            activeRootPackage = activeRootPackage,
            editableNodePackage = editableNodePackage,
            inputEditorPackage = inputEditorPackage,
            ownPackage = packageName
        )
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
        if (isSensitiveInputType(node.inputType)) return true
        val text = "${node.text?.toString().orEmpty()} ${node.hintText?.toString().orEmpty()} ${node.contentDescription?.toString().orEmpty()}".lowercase()
        val sensitiveTerms = listOf("password", "passcode", "pin", "otp", "one-time", "credit card", "card number", "cvv", "cvc")
        return sensitiveTerms.any { it in text }
    }

    private fun isSensitiveInputType(inputType: Int): Boolean {
        return isSecretInputType(inputType)
    }

    private fun showOverlay(expanded: Boolean) {
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
            opacity = bubbleOpacity(),
            onBubbleClick = { startRecording(RecordingInteraction.CONFIRMED) },
            onPushToTalkStart = { startRecording(RecordingInteraction.PUSH_TO_TALK) },
            onPushToTalkCancel = { cancelRecording(silent = true) },
            onPushToTalkSubmit = { submitRecording() },
            onCancel = { cancelRecording() },
            onSubmit = { submitRecording() },
            onMove = { x, y -> moveOverlay(x, y) }
        )
        view.setPushToTalk(overlayPushToTalk)
        view.setExpanded(expanded)
        overlay = view

        val width = if (expanded) expandedWidthPx(compactSize, overlayPushToTalk) else compactSize
        val height = compactSize
        val savedX = repository.getBubbleX()
        val savedY = repository.getBubbleY()
        val bounds = bubbleBounds(compactSize, compactSize)
        val savedPlacement = savedBubblePlacement()
        val defaultPosition = BubblePlacement(
            edge = BubbleEdge.END,
            verticalFraction = defaultBubbleVerticalFraction(bounds)
        ).toPosition(bounds)
        val compactPosition = when {
            savedPlacement != null -> savedPlacement.toPosition(bounds)
            savedX >= 0 && savedY >= 0 -> clampBubblePosition(savedX, savedY, compactSize, compactSize).let { BubblePosition(it.first, it.second) }
            else -> defaultPosition
        }
        view.setOpensLeft(shouldExpandedOverlayOpenLeft(compactPosition.x, compactSize))
        compactAnchorX = compactPosition.x
        compactAnchorY = compactPosition.y
        val position = if (expanded) {
            positionForExpandedOverlay(compactPosition.x, compactPosition.y, width, height)
        } else {
            compactPosition.x to compactPosition.y
        }
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
        if (!expanded) saveBubblePosition(params.x, params.y, compactSize, compactSize)
        overlayParams = params
        runCatching { windowManager.addView(view, params) }
    }

    private fun updateOverlaySize(expanded: Boolean) {
        val view = overlay ?: return
        val params = overlayParams ?: return
        val compactSize = compactSizePx()
        view.setCompactSize(compactSize)
        view.setBubbleOpacity(bubbleOpacity())
        view.setPushToTalk(overlayPushToTalk)
        view.setExpanded(expanded)
        val width = if (expanded) expandedWidthPx(compactSize, overlayPushToTalk) else compactSize
        val height = compactSize
        val savedCompactPosition = savedBubblePlacement()?.toPosition(bubbleBounds(compactSize, compactSize))
        val position = if (expanded) {
            val anchorX = savedCompactPosition?.x ?: compactAnchorX.takeIf { it >= 0 } ?: params.x
            val anchorY = savedCompactPosition?.y ?: compactAnchorY.takeIf { it >= 0 } ?: params.y
            view.setOpensLeft(shouldExpandedOverlayOpenLeft(anchorX, compactSize))
            positionForExpandedOverlay(anchorX, anchorY, width, height)
        } else {
            savedCompactPosition?.let { it.x to it.y } ?: run {
                val anchorX = compactAnchorX.takeIf { it >= 0 } ?: params.x
                val anchorY = compactAnchorY.takeIf { it >= 0 } ?: params.y
                clampBubblePosition(anchorX, anchorY, width, height)
            }
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
            saveBubblePosition(params.x, params.y, width, height)
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun hideOverlay() {
        val view = overlay ?: return
        overlay = null
        overlayParams = null
        overlayExpanded = false
        overlayPushToTalk = false
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
            saveBubblePosition(clamped.first, clamped.second, params.width, params.height)
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun compactSizePx(): Int = dp(repository.getBubbleSizeDp())

    private fun bubbleOpacity(): Float = repository.getBubbleOpacityPercent() / 100f

    private fun expandedWidthPx(compactSize: Int, pushToTalk: Boolean): Int =
        (compactSize * if (pushToTalk) 3.05f else 3.6f).toInt()

    private fun edgePaddingPx(): Int = dp(12)

    private fun savedBubblePlacement(): BubblePlacement? {
        val edge = when (repository.getBubbleEdge()) {
            BubbleEdge.START.name -> BubbleEdge.START
            BubbleEdge.END.name -> BubbleEdge.END
            else -> return null
        }
        val verticalFraction = repository.getBubbleVerticalFraction() ?: return null
        return BubblePlacement(edge, verticalFraction)
    }

    private fun saveBubblePosition(x: Int, y: Int, width: Int, height: Int) {
        repository.setBubblePosition(x, y)
        val placement = BubblePlacement.fromPosition(x, y, bubbleBounds(width, height))
        repository.setBubblePlacement(placement.edge.name, placement.verticalFraction)
    }

    private fun bubbleBounds(width: Int, height: Int): BubbleBounds {
        val bounds = screenBounds()
        return BubbleBounds(
            width = bounds.width(),
            height = bounds.height(),
            bubbleWidth = width,
            bubbleHeight = height,
            edgePadding = edgePaddingPx()
        )
    }

    private fun defaultBubbleVerticalFraction(bounds: BubbleBounds): Float {
        val yRange = bounds.maxY - bounds.minY
        if (yRange <= 0) return 0f
        return ((bounds.height / 3) - bounds.minY).toFloat() / yRange
    }

    private fun positionForExpandedOverlay(compactX: Int, compactY: Int, expandedWidth: Int, expandedHeight: Int): Pair<Int, Int> {
        val compactSize = compactSizePx()
        val opensLeft = shouldExpandedOverlayOpenLeft(compactX, compactSize)
        val desiredX = if (opensLeft) compactX + compactSize - expandedWidth else compactX
        return clampBubblePosition(desiredX, compactY, expandedWidth, expandedHeight)
    }

    private fun shouldExpandedOverlayOpenLeft(compactX: Int, compactSize: Int): Boolean {
        return compactX + compactSize / 2 > screenBounds().width() / 2
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

    private fun startRecording(interaction: RecordingInteraction) {
        if (recorder.isRecording) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            toast("Microphone permission is required")
            return
        }
        val config = repository.getPipelineConfig()
        val activePackage = activeApplicationPackage()
        val styleResolution = repository.resolveStyleForPackage(activePackage)
        val dictionary = repository.listDictionary().map { it.phrase }
        val validation = runCatching {
            PipelineExecutor(keyProvider = { secretStore.getApiKey(it) }).validate(config)
        }.exceptionOrNull()
        if (validation != null) {
            toast(validation.message ?: "Complete model setup in VoiceSlip")
            return
        }
        val id = UUID.randomUUID().toString()
        val file = File(repository.recordingsDir, "$id.wav")
        runCatching {
            recorder.start(file)
            haptic()
            recordingStartedAt = System.currentTimeMillis()
            currentConfigSnapshot = config
            currentStyleSnapshot = styleResolution
            currentInteraction = interaction
            overlayPushToTalk = interaction == RecordingInteraction.PUSH_TO_TALK
            currentItem = HistoryItem(
                id = id,
                createdAtMillis = recordingStartedAt,
                audioPath = file.absolutePath,
                durationMillis = 0L,
                status = RecordingStatus.RECORDING,
                pipelineMode = config.mode.name,
                stylePreset = styleResolution.styleName,
                pipelineSummary = pipelineSummary(config),
                targetPackage = styleResolution.targetPackage,
                targetAppLabel = styleResolution.targetAppLabel,
                resolvedCategoryId = styleResolution.categoryId,
                resolvedCategoryName = styleResolution.categoryName,
                resolvedStyleId = styleResolution.styleId,
                resolvedStyleName = styleResolution.styleName,
                stylePromptSnapshot = styleResolution.stylePrompt,
                dictionarySnapshot = org.json.JSONArray(dictionary).toString(),
                pipelineConfigSnapshot = repository.pipelineConfigSnapshot(config),
                dictionaryRoutingSnapshot = repository.dictionaryRoutingSnapshot(config, dictionary)
            ).also { repository.upsertHistory(it) }
            showOverlay(expanded = true)
            overlay?.setRecordingState(RecordingUiState.RECORDING)
            setOverlayKeepScreenOn(true)
            mainHandler.post(tickRunnable)
        }.onFailure {
            setOverlayKeepScreenOn(false)
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

    private fun cancelRecording(silent: Boolean = false) {
        mainHandler.removeCallbacks(tickRunnable)
        if (!silent) haptic()
        recorder.cancel()
        setOverlayKeepScreenOn(false)
        currentItem?.let { repository.deleteHistory(it.id) }
        currentItem = null
        currentConfigSnapshot = null
        currentStyleSnapshot = null
        currentInteraction = RecordingInteraction.CONFIRMED
        overlayPushToTalk = false
        refreshOverlayVisibility()
    }

    private fun submitRecording() {
        if (!recorder.isRecording) return
        mainHandler.removeCallbacks(tickRunnable)
        haptic()
        val result = recorder.stop() ?: return
        setOverlayKeepScreenOn(false)
        if (currentInteraction == RecordingInteraction.PUSH_TO_TALK && result.durationMillis < MIN_PUSH_TO_TALK_RECORDING_MS) {
            currentItem?.let { repository.deleteHistory(it.id) }
            currentItem = null
            currentConfigSnapshot = null
            currentStyleSnapshot = null
            currentInteraction = RecordingInteraction.CONFIRMED
            overlayPushToTalk = false
            hideOverlay()
            toast("Transcription cancelled")
            refreshOverlayVisibility()
            return
        }
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
        showOverlay(expanded = true)
        overlay?.setRecordingState(RecordingUiState.TRANSCRIBING)
        Thread {
            val updated = runCatching {
                val config = currentConfigSnapshot ?: repository.getPipelineConfig()
                val style = currentStyleSnapshot ?: repository.resolveStyleForPackage(item.targetPackage)
                val dictionary = repository.listDictionary().map { it.phrase }
                val transcriptionDictionary = if (config.mode == PipelineMode.AUDIO_DIRECT) {
                    dictionary
                } else {
                    val plan = repository.dictionaryPlanForTranscription(config, dictionary)
                    dictionary.take(plan.includedTerms)
                }
                val pipeline = PipelineExecutor(
                    keyProvider = { secretStore.getApiKey(it) },
                    openRouterProviderSort = { repository.getOpenRouterProviderSort() },
                    openRouterReasoningEffort = { repository.getOpenRouterReasoningEffort() },
                    openRouterModelLookup = { modelId ->
                        (repository.getCachedModels(com.example.voiceslip.data.ProviderId.OPENROUTER) + repository.getCachedOpenRouterAudioModels())
                            .firstOrNull { it.id == modelId }
                    }
                ).execute(
                    config,
                    result.file,
                    dictionary,
                    transcriptionDictionaryTerms = transcriptionDictionary,
                    styleId = style.styleId,
                    styleName = style.styleName,
                    stylePrompt = style.stylePrompt,
                    cleanupPolicy = repository.getCleanupPolicy(),
                    languageHints = repository.getLanguageHints(),
                    preserveSpokenLanguage = repository.getPreserveSpokenLanguage()
                )
                val text = pipeline.finalText
                outputGuardRejection(text, result.durationMillis)?.let { rejection ->
                    return@runCatching item.withPipelineResult(pipeline, config).copy(
                        status = RecordingStatus.FAILED,
                        transcript = null,
                        error = rejection,
                        errorStage = "output_guard",
                        targetPackage = style.targetPackage,
                        targetAppLabel = style.targetAppLabel,
                        resolvedCategoryId = style.categoryId,
                        resolvedCategoryName = style.categoryName,
                        resolvedStyleId = style.styleId,
                        resolvedStyleName = style.styleName,
                        stylePromptSnapshot = style.stylePrompt,
                        dictionarySnapshot = org.json.JSONArray(dictionary).toString(),
                        pipelineConfigSnapshot = repository.pipelineConfigSnapshot(config),
                        dictionaryRoutingSnapshot = repository.dictionaryRoutingSnapshot(config, dictionary)
                    )
                }
                val insertionResult = mainHandler.postAndWait { insertOrCopy(text) }
                item.copy(
                    status = RecordingStatus.SUCCEEDED,
                    transcript = text,
                    rawTranscript = pipeline.rawTranscript,
                    finalText = pipeline.finalText,
                    detectedLanguage = pipeline.detectedLanguage,
                    error = insertionResult.historyNote,
                    provider = pipeline.provider,
                    model = pipeline.model,
                    pipelineMode = config.mode.name,
                    transcriptionProvider = pipeline.transcriptionProvider,
                    transcriptionModel = pipeline.transcriptionModel,
                    audioModelProvider = pipeline.audioModelProvider,
                    audioModel = pipeline.audioModel,
                    postProcessingProvider = pipeline.postProcessingProvider,
                    postProcessingModel = pipeline.postProcessingModel,
                    stylePreset = pipeline.stylePreset,
                    pipelineSummary = pipeline.pipelineSummary,
                    errorStage = insertionResult.errorStage,
                    metadataJson = pipeline.metadataJson,
                    targetPackage = style.targetPackage,
                    targetAppLabel = style.targetAppLabel,
                    resolvedCategoryId = style.categoryId,
                    resolvedCategoryName = style.categoryName,
                    resolvedStyleId = style.styleId,
                    resolvedStyleName = style.styleName,
                    stylePromptSnapshot = style.stylePrompt,
                    dictionarySnapshot = org.json.JSONArray(dictionary).toString(),
                    pipelineConfigSnapshot = repository.pipelineConfigSnapshot(config),
                    dictionaryRoutingSnapshot = repository.dictionaryRoutingSnapshot(config, dictionary)
                )
            }.getOrElse { error ->
                item.copy(
                    status = RecordingStatus.FAILED,
                    error = error.message ?: error::class.java.simpleName,
                    errorStage = (error as? PipelineException)?.stage ?: "pipeline"
                )
            }
            repository.upsertHistory(updated)
            mainHandler.post {
                val completedPushToTalk = overlayPushToTalk
                currentItem = null
                currentConfigSnapshot = null
                currentStyleSnapshot = null
                currentInteraction = RecordingInteraction.CONFIRMED
                overlayPushToTalk = false
                if (completedPushToTalk) hideOverlay()
                if (updated.status == RecordingStatus.SUCCEEDED) {
                    insertionToast(updated.errorStage)?.let { toast(it) }
                } else {
                    toast("Transcription failed. Open VoiceSlip to retry.")
                }
                refreshOverlayVisibility()
            }
        }.start()
    }

    private fun insertOrCopy(text: String): InsertionResult {
        val node = findFocusedEditableNode()
        if (node != null && isSensitiveNode(node)) {
            Log.d(TAG, "Insertion blocked: sensitive accessibility node")
            return InsertionResult.FAILED_SENSITIVE_FIELD
        }

        val inputMethod = accessibilityInputMethod
        if (inputMethod?.isSensitiveEditor() == true) {
            Log.d(TAG, "Insertion blocked: sensitive input editor")
            return InsertionResult.FAILED_SENSITIVE_FIELD
        }

        val tryInputMethodFirst = shouldTryAccessibilityInputMethodBeforeFocusedNode(node != null)
        if (tryInputMethodFirst) {
            inputMethod?.let {
                if (it.commitText(text)) {
                    Log.d(TAG, "Insertion attempted via accessibility input method")
                    return InsertionResult.INSERTED_VIA_INPUT_METHOD
                }
            }
        }

        if (node != null) {
            if (insertDirectly(node, text)) {
                Log.d(TAG, "Insertion succeeded via ACTION_SET_TEXT")
                return InsertionResult.INSERTED_DIRECT
            }

            if (node.supportsAction(AccessibilityNodeInfo.ACTION_PASTE)) {
                copyToClipboard(text)
                if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
                    Log.d(TAG, "Insertion succeeded via clipboard paste fallback")
                    return InsertionResult.INSERTED_VIA_CLIPBOARD
                }
            }
        }

        if (!tryInputMethodFirst) {
            inputMethod?.let {
                if (it.commitText(text)) {
                    Log.d(TAG, "Insertion attempted via accessibility input method")
                    return InsertionResult.INSERTED_VIA_INPUT_METHOD
                }
            }
        }

        if (node == null || !hasInputMethodWindow()) {
            copyToClipboard(text)
            Log.d(TAG, "Insertion copied to clipboard: no target node or input method window")
            return InsertionResult.COPIED_NO_TARGET
        }

        Log.d(TAG, "Insertion failed")
        return InsertionResult.FAILED_INSERTION
    }

    private fun insertDirectly(node: AccessibilityNodeInfo, text: String): Boolean {
        if (!node.supportsAction(AccessibilityNodeInfo.ACTION_SET_TEXT)) return false
        val currentText = node.text?.toString().orEmpty()
        val selectionStart = node.textSelectionStart
        val selectionEnd = node.textSelectionEnd
        if (selectionStart < 0 || selectionEnd < 0) {
            return if (shouldSetTextWithoutSelection(currentText, node.hintText?.toString())) {
                setNodeText(node, text)
            } else {
                false
            }
        }

        val start = min(selectionStart, selectionEnd).coerceIn(0, currentText.length)
        val end = max(selectionStart, selectionEnd).coerceIn(0, currentText.length)
        val updatedText = currentText.substring(0, start) + text + currentText.substring(end)
        return setNodeText(node, updatedText)
    }

    private fun setNodeText(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun AccessibilityNodeInfo.supportsAction(actionId: Int): Boolean {
        return actionList.any { it.id == actionId }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("VoiceSlip transcription", text))
    }

    private fun insertionToast(errorStage: String?): String? {
        return when (errorStage) {
            null -> null
            "clipboard_fallback" -> null
            "no_editable_target" -> "Copied transcription"
            "sensitive_field" -> "Cannot insert into sensitive fields"
            else -> "Could not insert transcription"
        }
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

    private fun setOverlayKeepScreenOn(enabled: Boolean) {
        val view = overlay ?: return
        val params = overlayParams ?: return
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val updatedFlags = if (enabled) params.flags or flag else params.flags and flag.inv()
        if (params.flags == updatedFlags) return
        params.flags = updatedFlags
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val MAX_RECORDING_MS = 5 * 60 * 1000L
        private const val MIN_PUSH_TO_TALK_RECORDING_MS = 800L
        private const val TAG = "VoiceSlip"
        var instance: VoiceSlipAccessibilityService? = null
            private set
    }

    private fun pipelineSummary(config: com.imdinkie.voiceslip.data.PipelineConfig): String {
        return when (config.mode) {
            PipelineMode.PURE_TRANSCRIPTION -> config.transcriptionDisplayName()
            PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING ->
                "${config.transcriptionDisplayName()} -> ${config.postProcessingProvider.label} ${config.postProcessingModel}"
            PipelineMode.AUDIO_DIRECT -> config.audioDirectDisplayName()
        }
    }
}

private enum class RecordingInteraction {
    CONFIRMED,
    PUSH_TO_TALK
}

private fun HistoryItem.withPipelineResult(result: PipelineResult, config: PipelineConfig): HistoryItem = copy(
    rawTranscript = result.rawTranscript,
    finalText = result.finalText,
    detectedLanguage = result.detectedLanguage,
    provider = result.provider,
    model = result.model,
    pipelineMode = config.mode.name,
    transcriptionProvider = result.transcriptionProvider,
    transcriptionModel = result.transcriptionModel,
    audioModelProvider = result.audioModelProvider,
    audioModel = result.audioModel,
    postProcessingProvider = result.postProcessingProvider,
    postProcessingModel = result.postProcessingModel,
    stylePreset = result.stylePreset,
    pipelineSummary = result.pipelineSummary,
    metadataJson = result.metadataJson
)

private fun <T> Handler.postAndWait(block: () -> T): T {
    if (Looper.myLooper() == Looper.getMainLooper()) return block()
    val lock = Object()
    var complete = false
    var result: T? = null
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
    @Suppress("UNCHECKED_CAST")
    return result as T
}

private enum class InsertionResult(
    val historyNote: String?,
    val errorStage: String?
) {
    INSERTED_DIRECT(null, null),
    INSERTED_VIA_INPUT_METHOD(null, null),
    INSERTED_VIA_CLIPBOARD("Inserted using clipboard paste fallback because direct insertion was unavailable.", "clipboard_fallback"),
    COPIED_NO_TARGET("Copied to clipboard because no editable field was available.", "no_editable_target"),
    FAILED_SENSITIVE_FIELD("Did not insert or copy because the focused field appears sensitive.", "sensitive_field"),
    FAILED_INSERTION("Could not insert into the focused field.", "insertion_failed")
}

private class VoiceSlipInputMethod(service: VoiceSlipAccessibilityService) : InputMethod(service) {
    fun targetPackageName(): String? = currentInputEditorInfo?.packageName

    fun commitText(text: String): Boolean {
        if (!currentInputStarted) return false
        val connection = currentInputConnection ?: return false
        return runCatching {
            connection.commitText(text, 1, null as TextAttribute?)
            true
        }.getOrDefault(false)
    }

    fun isSensitiveEditor(): Boolean {
        val editorInfo = currentInputEditorInfo ?: return false
        return isSecretInputType(editorInfo.inputType)
    }

}

internal fun shouldShowBubbleForField(
    secretField: Boolean
): Boolean = !secretField

internal fun shouldBlockInsertionForField(
    secretAccessibilityNode: Boolean,
    secretInputEditor: Boolean
): Boolean = secretAccessibilityNode || secretInputEditor

internal fun shouldTryAccessibilityInputMethodBeforeFocusedNode(hasFocusedEditableNode: Boolean): Boolean =
    !hasFocusedEditableNode

internal fun shouldSetTextWithoutSelection(currentText: String, hintText: String?): Boolean =
    currentText.isEmpty() || currentText == hintText

internal fun isSecretInputType(inputType: Int): Boolean {
    val textVariation = inputType and (InputType.TYPE_MASK_CLASS or InputType.TYPE_MASK_VARIATION)
    return textVariation == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) ||
        textVariation == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) ||
        textVariation == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) ||
        textVariation == (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
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
        val barWidth = size * 0.07f
        val gap = size * 0.07f
        val heights = floatArrayOf(0.21f, 0.39f, 0.29f, 0.5f, 0.18f)
        val totalWidth = barWidth * heights.size + gap * (heights.size - 1)
        var left = centerX - totalWidth / 2f
        heights.forEach { heightRatio ->
            val barHeight = size * heightRatio
            canvas.drawRoundRect(
                left,
                centerY - barHeight / 2f,
                left + barWidth,
                centerY + barHeight / 2f,
                barWidth / 2f,
                barWidth / 2f,
                paint
            )
            left += barWidth + gap
        }
    }
}

private class PushToTalkTargetView(context: Context) : View(context) {
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(186, 174, 196)
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(186, 174, 196)
        style = Paint.Style.FILL
    }
    private var loading = false

    fun setLoading(value: Boolean) {
        if (loading == value) return
        loading = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = min(width, height) * 0.28f
        val centerX = width / 2f
        val centerY = height / 2f
        if (loading) {
            ringPaint.color = Color.rgb(28, 24, 32)
            val inset = min(width, height) / 2f - radius
            canvas.drawArc(inset, inset, width - inset, height - inset, -80f, 300f, false, ringPaint)
        } else {
            ringPaint.color = Color.rgb(186, 174, 196)
            canvas.drawCircle(centerX, centerY, radius, ringPaint)
            canvas.drawCircle(centerX, centerY, radius * 0.58f, dotPaint)
        }
    }
}

private class RecordingOverlay(
    context: Context,
    compactSizePx: Int,
    opacity: Float,
    private val onBubbleClick: () -> Unit,
    private val onPushToTalkStart: () -> Unit,
    private val onPushToTalkCancel: () -> Unit,
    private val onPushToTalkSubmit: () -> Unit,
    private val onCancel: () -> Unit,
    private val onSubmit: () -> Unit,
    private val onMove: (Int, Int) -> Unit
) : FrameLayout(context) {
    private val collapsed = BubbleIconView(context)
    private val expanded = LinearLayout(context)
    private val pushToTalkTarget = PushToTalkTargetView(context)
    private val waveform = WaveformView(context)
    private var compactSize = compactSizePx
    private val cancel = circle("×", Color.rgb(183, 178, 190), Color.rgb(28, 24, 32))
    private val submit = circle("✓", Color.rgb(88, 37, 115), Color.WHITE)
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var isExpanded = false
    private var pushToTalk = false
    private var opensLeft = false
    private var uiState = RecordingUiState.IDLE
    private val gesture = RecordingGestureController()
    private val longPressRunnable = Runnable {
        if (gesture.onLongPressTimeout(System.currentTimeMillis()) == RecordingGestureAction.StartPushToTalk) {
            onPushToTalkStart()
        }
    }

    init {
        alpha = opacity
        setPadding(dp(4), dp(4), dp(4), dp(4))
        collapsed.background = bubbleBg(Color.rgb(194, 184, 205), dp(22).toFloat())
        addView(collapsed, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        expanded.orientation = LinearLayout.HORIZONTAL
        expanded.gravity = Gravity.CENTER
        expanded.visibility = GONE
        addView(expanded, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        waveform.background = bubbleBg(Color.rgb(194, 184, 205), dp(24).toFloat())
        rebuildExpandedChildren()
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
                    gesture.onDown(event.rawX, event.rawY, startX, startY, System.currentTimeMillis())
                    if (!isExpanded) postDelayed(longPressRunnable, PUSH_TO_TALK_LONG_PRESS_MS)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    when (val action = gesture.onMove(event.rawX, event.rawY, System.currentTimeMillis())) {
                        is RecordingGestureAction.MoveBubble -> onMove(action.x, action.y)
                        else -> Unit
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    removeCallbacks(longPressRunnable)
                    when (gesture.onUp(event.rawX, event.rawY, System.currentTimeMillis())) {
                        RecordingGestureAction.StartConfirmedRecording -> if (!isExpanded) onBubbleClick()
                        RecordingGestureAction.SubmitPushToTalk -> onPushToTalkSubmit()
                        else -> Unit
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    removeCallbacks(longPressRunnable)
                    if (pushToTalk) onPushToTalkCancel()
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
        updateExpandedChildSizes()
    }

    fun setPushToTalk(value: Boolean) {
        if (pushToTalk == value) return
        pushToTalk = value
        updatePushToTalkAppearance()
        rebuildExpandedChildren()
        updateExpandedChildSizes()
    }

    fun setOpensLeft(value: Boolean) {
        if (opensLeft == value) return
        opensLeft = value
        rebuildExpandedChildren()
    }

    fun setCompactSize(sizePx: Int) {
        if (compactSize == sizePx) return
        compactSize = sizePx
        updateExpandedChildSizes()
        collapsed.invalidate()
        waveform.invalidate()
    }

    fun setBubbleOpacity(opacity: Float) {
        alpha = opacity.coerceIn(0.2f, 1f)
    }

    fun setRecordingState(state: RecordingUiState) {
        uiState = state
        waveform.setTranscribing(state == RecordingUiState.TRANSCRIBING)
        pushToTalkTarget.setLoading(state == RecordingUiState.TRANSCRIBING)
        updatePushToTalkAppearance()
        val controlsEnabled = state == RecordingUiState.RECORDING
        cancel.isEnabled = controlsEnabled
        submit.isEnabled = controlsEnabled
        cancel.alpha = if (controlsEnabled) 1f else 0.45f
        submit.alpha = if (controlsEnabled) 1f else 0.45f
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
        if (pushToTalk) {
            pushToTalkTarget.layoutParams = LinearLayout.LayoutParams(control, control).apply {
                marginStart = if (opensLeft) gap else 0
                marginEnd = if (opensLeft) 0 else gap
            }
            waveform.layoutParams = LinearLayout.LayoutParams((compactSize * 1.7f).toInt(), control)
        } else {
            cancel.layoutParams = LinearLayout.LayoutParams(control, control)
            waveform.layoutParams = LinearLayout.LayoutParams(0, control, 1f).apply {
                marginStart = gap
                marginEnd = gap
            }
            submit.layoutParams = LinearLayout.LayoutParams(control, control)
        }
    }

    private fun rebuildExpandedChildren() {
        expanded.removeAllViews()
        if (pushToTalk) {
            if (opensLeft) {
                expanded.addView(waveform)
                expanded.addView(pushToTalkTarget)
            } else {
                expanded.addView(pushToTalkTarget)
                expanded.addView(waveform)
            }
        } else {
            expanded.addView(cancel)
            expanded.addView(waveform)
            expanded.addView(submit)
        }
    }

    private fun updatePushToTalkAppearance() {
        if (!pushToTalk) {
            expanded.background = null
            waveform.background = bubbleBg(Color.rgb(194, 184, 205), dp(24).toFloat())
            return
        }
        val backgroundColor = if (uiState == RecordingUiState.TRANSCRIBING) {
            Color.rgb(194, 184, 205)
        } else {
            Color.rgb(88, 37, 115)
        }
        expanded.background = bubbleBg(backgroundColor, dp(30).toFloat())
        waveform.background = null
    }

    private fun bubbleBg(color: Int, radius: Float) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val PUSH_TO_TALK_LONG_PRESS_MS = 400L
    }
}

private class WaveformView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(28, 24, 32)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 4f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        textSize = 15f * resources.displayMetrics.density
    }
    private var level = 0.02f
    private var countdown: Int? = null
    private var transcribing = false

    fun setAmplitude(amplitude: Int, countdownSeconds: Int?) {
        level = min(1f, max(0.02f, amplitude / 32767f))
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
        val bars = 11
        val gap = width / (bars + 1f)
        for (i in 1..bars) {
            val phase = if (i % 2 == 0) 0.55f else 1f
            val barHeight = (height * 0.06f) + (height * 0.42f * level * phase)
            val x = gap * i
            canvas.drawLine(x, centerY - barHeight / 2f, x, centerY + barHeight / 2f, paint)
        }
    }
}
