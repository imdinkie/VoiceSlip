package com.imdinkie.voiceslip

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.imdinkie.voiceslip.data.BUBBLE_OPACITY_MAX_PERCENT
import com.imdinkie.voiceslip.data.BUBBLE_OPACITY_MIN_PERCENT
import com.imdinkie.voiceslip.data.BUBBLE_SIZE_MAX_DP
import com.imdinkie.voiceslip.data.BUBBLE_SIZE_MIN_DP
import com.imdinkie.voiceslip.data.DictionaryEntry
import com.imdinkie.voiceslip.data.HistoryItem
import com.imdinkie.voiceslip.data.ModelOption
import com.imdinkie.voiceslip.data.OpenRouterEndpointDetails
import com.imdinkie.voiceslip.data.OpenRouterProviderSort
import com.imdinkie.voiceslip.data.OpenRouterReasoningEffort
import com.imdinkie.voiceslip.data.PipelineConfig
import com.imdinkie.voiceslip.data.PipelineMode
import com.imdinkie.voiceslip.data.PostProcessingProvider
import com.imdinkie.voiceslip.data.ProviderId
import com.imdinkie.voiceslip.data.RecordingStatus
import com.imdinkie.voiceslip.data.SecretStore
import com.imdinkie.voiceslip.data.CATEGORY_OTHER
import com.imdinkie.voiceslip.data.EngineKind
import com.imdinkie.voiceslip.data.EngineDictionaryRoutingEntity
import com.imdinkie.voiceslip.data.OPENROUTER_AUDIO_TRANSCRIPTION_ROUTING_ID
import com.imdinkie.voiceslip.data.STYLE_CASUAL
import com.imdinkie.voiceslip.data.VoiceCategory
import com.imdinkie.voiceslip.data.VoiceSlipRepository
import com.imdinkie.voiceslip.data.VoiceStyle
import com.imdinkie.voiceslip.audio.AudioDerivativeConverter
import com.imdinkie.voiceslip.audio.audioFileFormat
import com.imdinkie.voiceslip.audio.recordingFormatFor
import com.imdinkie.voiceslip.audio.requiredUploadFormatFor
import com.imdinkie.voiceslip.net.GroqClient
import com.imdinkie.voiceslip.net.GitHubRelease
import com.imdinkie.voiceslip.net.GitHubReleasesClient
import com.imdinkie.voiceslip.net.OpenRouterClient
import com.imdinkie.voiceslip.net.PipelineException
import com.imdinkie.voiceslip.net.PipelineExecutor
import com.imdinkie.voiceslip.net.PipelineResult
import com.imdinkie.voiceslip.net.buildAudioDirectPrompt
import com.imdinkie.voiceslip.net.buildAudioTranscriptionPromptPreview
import com.imdinkie.voiceslip.net.buildLanguageHintExamples
import com.imdinkie.voiceslip.net.buildPostProcessingSystemPrompt
import com.imdinkie.voiceslip.net.buildPostProcessingUserPrompt
import com.imdinkie.voiceslip.net.isReleaseNewer
import com.imdinkie.voiceslip.net.outputGuardRejection
import com.imdinkie.voiceslip.service.VoiceSlipAccessibilityService
import com.imdinkie.voiceslip.ui.theme.VoiceSlipTheme
import org.json.JSONObject
import java.io.File
import java.util.Date
import kotlin.math.roundToInt

private const val SETUP_TAB_INDEX = 0
private const val HISTORY_TAB_INDEX = 1
private const val MODELS_TAB_INDEX = 2
private const val STYLE_TAB_INDEX = 3
private const val DICTIONARY_TAB_INDEX = 4
private val VOICESLIP_TABS = listOf("Setup", "History", "Models", "Style", "Dictionary")

class MainActivity : ComponentActivity() {
    private lateinit var repository: VoiceSlipRepository
    private lateinit var secretStore: SecretStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = VoiceSlipRepository(this)
        secretStore = SecretStore(this)
        enableEdgeToEdge()
        setContent {
            VoiceSlipTheme {
                VoiceSlipApp(
                    repository = repository,
                    secretStore = secretStore,
                    onOpenAccessibility = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenOverlay = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    },
                    onRetry = { retryTranscription(it) },
                    onCopy = { copyToClipboard(it) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun retryTranscription(item: HistoryItem) {
        val config = repository.getPipelineConfig()
        repository.upsertHistory(item.copy(status = RecordingStatus.TRANSCRIBING, error = null))
        Thread {
            val updated = runCatching {
                val uploadFile = AudioDerivativeConverter().fileForUpload(File(item.audioPath), requiredUploadFormatFor(config))
                val dictionary = repository.listDictionary().map { it.phrase }
                val transcriptionDictionary = if (config.mode == PipelineMode.AUDIO_DIRECT) {
                    dictionary
                } else {
                    val plan = repository.dictionaryPlanForTranscription(config, dictionary)
                    dictionary.take(plan.includedTerms)
                }
                val result = PipelineExecutor(
                    keyProvider = { secretStore.getApiKey(it) },
                    openRouterProviderSort = { repository.getOpenRouterProviderSort() },
                    openRouterModelLookup = { modelId ->
                        (repository.getCachedModels(ProviderId.OPENROUTER) + repository.getCachedOpenRouterAudioModels())
                            .firstOrNull { it.id == modelId }
                    }
                ).execute(
                    config = config,
                    audioFile = uploadFile,
                    dictionaryTerms = dictionary,
                    transcriptionDictionaryTerms = transcriptionDictionary,
                    styleId = item.resolvedStyleId ?: STYLE_CASUAL,
                    styleName = item.resolvedStyleName ?: "Casual",
                    stylePrompt = item.stylePromptSnapshot ?: repository.getStyle(STYLE_CASUAL).effectivePrompt,
                    cleanupPolicy = repository.getCleanupPolicy(),
                    languageHints = repository.getLanguageHints(),
                    preserveSpokenLanguage = repository.getPreserveSpokenLanguage()
                )
                outputGuardRejection(result.finalText, item.durationMillis)?.let { rejection ->
                    return@runCatching item.withPipelineResult(result, config, item.retryCount + 1).copy(
                        status = RecordingStatus.FAILED,
                        transcript = null,
                        error = rejection,
                        errorStage = "output_guard"
                    )
                }
                item.copy(
                    status = RecordingStatus.SUCCEEDED,
                    transcript = result.finalText,
                    rawTranscript = result.rawTranscript,
                    finalText = result.finalText,
                    detectedLanguage = result.detectedLanguage,
                    error = null,
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
                    errorStage = null,
                    metadataJson = audioMetadata(result.metadataJson, item.audioPath, uploadFile),
                    retryCount = item.retryCount + 1
                )
            }.getOrElse {
                if (audioFileFormat(File(item.audioPath))?.name == "M4A" && isAudioFormatFailure(it)) {
                    repository.rememberWavForAudioConsumer(config)
                }
                item.copy(
                    status = RecordingStatus.FAILED,
                    error = it.message ?: it::class.java.simpleName,
                    errorStage = (it as? PipelineException)?.stage ?: "pipeline",
                    retryCount = item.retryCount + 1
                )
            }
            repository.upsertHistory(updated)
        }.start()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("VoiceSlip transcription", text))
    }
}

private fun audioMetadata(existing: String?, originalPath: String, uploadFile: File): String {
    val original = File(originalPath)
    val metadata = JSONObject()
        .put("type", "voiceslip_audio")
        .put("originalFile", original.name)
        .put("originalFormat", audioFileFormat(original)?.label ?: original.extension)
        .put("uploadedFile", uploadFile.name)
        .put("uploadedFormat", audioFileFormat(uploadFile)?.label ?: uploadFile.extension)
        .put("conversionUsed", original.absolutePath != uploadFile.absolutePath)
    return listOfNotNull(existing?.takeIf { it.isNotBlank() }, metadata.toString()).joinToString("\n")
}

private fun isAudioFormatFailure(error: Throwable): Boolean {
    val message = error.message.orEmpty().lowercase()
    return listOf("audio format", "invalid audio", "failed to load audio", "valid mp3 or wav", "unsupported format").any { it in message }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSlipApp(
    repository: VoiceSlipRepository,
    secretStore: SecretStore,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRetry: (HistoryItem) -> Unit,
    onCopy: (String) -> Unit
) {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }
    var resumeTick by remember { mutableIntStateOf(0) }
    var mistralKey by remember { mutableStateOf(secretStore.getApiKey(ProviderId.MISTRAL).orEmpty()) }
    var groqKey by remember { mutableStateOf(secretStore.getApiKey(ProviderId.GROQ).orEmpty()) }
    var openRouterKey by remember { mutableStateOf(secretStore.getApiKey(ProviderId.OPENROUTER).orEmpty()) }
    var elevenLabsKey by remember { mutableStateOf(secretStore.getApiKey(ProviderId.ELEVENLABS).orEmpty()) }
    var pipelineConfig by remember { mutableStateOf(repository.getPipelineConfig()) }
    var languageHints by remember { mutableStateOf(repository.getLanguageHints()) }
    var preserveSpokenLanguage by remember { mutableStateOf(repository.getPreserveSpokenLanguage()) }
    var groqModels by remember { mutableStateOf(repository.getCachedModels(ProviderId.GROQ)) }
    var openRouterModels by remember { mutableStateOf(repository.getCachedModels(ProviderId.OPENROUTER)) }
    var openRouterAudioModels by remember { mutableStateOf(repository.getCachedOpenRouterAudioModels()) }
    var openRouterProviderSort by remember { mutableStateOf(repository.getOpenRouterProviderSort()) }
    var modelStatus by remember { mutableStateOf<String?>(null) }
    var appEnabled by remember { mutableStateOf(repository.getAppEnabled()) }
    var haptics by remember { mutableStateOf(repository.getHapticsEnabled()) }
    var bubbleSizeDp by remember { mutableIntStateOf(repository.getBubbleSizeDp()) }
    var bubbleOpacityPercent by remember { mutableIntStateOf(repository.getBubbleOpacityPercent()) }
    var availableRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var updateCheckRunning by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember {
        mutableIntStateOf(
            if (initialSetupStatus(context, repository, secretStore).ready) HISTORY_TAB_INDEX else SETUP_TAB_INDEX
        )
    }
    var historyScrollRequest by remember { mutableIntStateOf(0) }
    var lastViewedHistoryTopId by remember { mutableStateOf(repository.listHistory().firstOrNull()?.id) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTick++
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTick++
    }

    LaunchedEffect(refreshTick) {
        mistralKey = secretStore.getApiKey(ProviderId.MISTRAL).orEmpty()
        groqKey = secretStore.getApiKey(ProviderId.GROQ).orEmpty()
        openRouterKey = secretStore.getApiKey(ProviderId.OPENROUTER).orEmpty()
        elevenLabsKey = secretStore.getApiKey(ProviderId.ELEVENLABS).orEmpty()
        pipelineConfig = repository.getPipelineConfig()
        languageHints = repository.getLanguageHints()
        preserveSpokenLanguage = repository.getPreserveSpokenLanguage()
        groqModels = repository.getCachedModels(ProviderId.GROQ)
        openRouterModels = repository.getCachedModels(ProviderId.OPENROUTER)
        openRouterAudioModels = repository.getCachedOpenRouterAudioModels()
        openRouterProviderSort = repository.getOpenRouterProviderSort()
        appEnabled = repository.getAppEnabled()
        haptics = repository.getHapticsEnabled()
        bubbleSizeDp = repository.getBubbleSizeDp()
        bubbleOpacityPercent = repository.getBubbleOpacityPercent()
    }
    DisposableEffect(context) {
        val lifecycleOwner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
                resumeTick++
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }
    val setupStatus = remember(refreshTick, mistralKey, groqKey, openRouterKey, elevenLabsKey, pipelineConfig) {
        currentSetupStatus(
            context = context,
            config = pipelineConfig,
            keys = mapOf(
                ProviderId.MISTRAL to mistralKey,
                ProviderId.GROQ to groqKey,
                ProviderId.OPENROUTER to openRouterKey,
                ProviderId.ELEVENLABS to elevenLabsKey
            )
        )
    }

    fun checkForUpdates(force: Boolean, reportWhenCurrent: Boolean) {
        if ((!force && !repository.shouldCheckForUpdates()) || updateCheckRunning) return
        updateCheckRunning = true
        if (reportWhenCurrent) updateStatus = "Checking GitHub releases..."
        Thread {
            val result = runCatching {
                val release = GitHubReleasesClient().latestStableRelease()
                val installedVersion = installedVersionName(context)
                if (
                    isReleaseNewer(installedVersion, release.tagName) &&
                    !repository.isReleaseDismissed(release.tagName)
                ) {
                    release
                } else {
                    null
                }
            }
            repository.markUpdateChecked()
            (context as? ComponentActivity)?.runOnUiThread {
                result.fold(
                    onSuccess = { release ->
                        if (release != null) {
                            availableRelease = release
                            updateStatus = "Update found: ${release.tagName}"
                        } else if (reportWhenCurrent) {
                            updateStatus = "VoiceSlip is up to date."
                        }
                    },
                    onFailure = {
                        if (reportWhenCurrent) {
                            updateStatus = it.message ?: "Update check failed. Try again later."
                        }
                    }
                )
                updateCheckRunning = false
            }
        }.start()
    }

    LaunchedEffect(selectedTab, resumeTick) {
        if (selectedTab == HISTORY_TAB_INDEX) {
            val topId = repository.listHistory().firstOrNull()?.id
            if (topId != null && topId != lastViewedHistoryTopId) {
                historyScrollRequest++
            }
            lastViewedHistoryTopId = topId
        }
    }
    LaunchedEffect(resumeTick) {
        checkForUpdates(force = false, reportWhenCurrent = false)
    }

    fun refreshOpenRouterEndpointDetails(modelIds: List<String>) {
        val cleanIds = modelIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanIds.isEmpty()) return
        Thread {
            cleanIds.forEach { modelId ->
                runCatching {
                    OpenRouterClient().endpointDetails(secretStore.getApiKey(ProviderId.OPENROUTER).orEmpty(), modelId)
                }.onSuccess { repository.setCachedOpenRouterEndpointDetails(it) }
            }
            (context as? ComponentActivity)?.runOnUiThread { refreshTick++ }
        }.start()
    }

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 36.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "VoiceSlip",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    StatusPill(
                        when {
                            !appEnabled -> "Off"
                            setupStatus.ready -> "Ready"
                            else -> "Setup"
                        }
                    )
                }
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                    VOICESLIP_TABS.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                SETUP_TAB_INDEX -> SetupScreen(
                    mistralKey = mistralKey,
                    groqKey = groqKey,
                    openRouterKey = openRouterKey,
                    elevenLabsKey = elevenLabsKey,
                    appEnabled = appEnabled,
                    haptics = haptics,
                    bubbleSizeDp = bubbleSizeDp,
                    bubbleOpacityPercent = bubbleOpacityPercent,
                    setupStatus = setupStatus,
                    updateStatus = updateStatus,
                    showUpdatePreview = isAppDebuggable(context),
                    onProviderKeyChange = { provider, key ->
                        when (provider) {
                            ProviderId.MISTRAL -> mistralKey = key
                            ProviderId.GROQ -> groqKey = key
                            ProviderId.OPENROUTER -> openRouterKey = key
                            ProviderId.ELEVENLABS -> elevenLabsKey = key
                        }
                        secretStore.saveApiKey(provider, key)
                    },
                    onAppEnabledChange = {
                        appEnabled = it
                        repository.setAppEnabled(it)
                        VoiceSlipAccessibilityService.instance?.refreshFromSettings()
                    },
                    onHapticsChange = {
                        haptics = it
                        repository.setHapticsEnabled(it)
                    },
                    onBubbleSizeChange = {
                        bubbleSizeDp = it
                        repository.setBubbleSizeDp(it)
                    },
                    onBubbleOpacityChange = {
                        bubbleOpacityPercent = it
                        repository.setBubbleOpacityPercent(it)
                    },
                    onOpenAccessibility = onOpenAccessibility,
                    onOpenOverlay = onOpenOverlay,
                    onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    onCheckUpdates = {
                        checkForUpdates(force = true, reportWhenCurrent = true)
                    },
                    onPreviewUpdate = {
                        updateStatus = "Showing update prompt preview."
                        availableRelease = GitHubRelease(
                            tagName = "v999.0.0",
                            name = "VoiceSlip test release",
                            htmlUrl = "https://github.com/imdinkie/VoiceSlip/releases",
                            publishedAt = ""
                        )
                    }
                )
                HISTORY_TAB_INDEX -> HistoryScreen(
                    repository = repository,
                    scrollToTopRequest = historyScrollRequest,
                    onRetry = onRetry,
                    onCopy = onCopy
                )
                MODELS_TAB_INDEX -> ModelsScreen(
                    config = pipelineConfig,
                    repository = repository,
                    languageHints = languageHints,
                    preserveSpokenLanguage = preserveSpokenLanguage,
                    groqModels = groqModels,
                    openRouterModels = openRouterModels,
                    openRouterAudioModels = openRouterAudioModels,
                    openRouterProviderSort = openRouterProviderSort,
                    modelStatus = modelStatus,
                    hasMistralKey = mistralKey.isNotBlank(),
                    hasGroqKey = groqKey.isNotBlank(),
                    hasOpenRouterKey = openRouterKey.isNotBlank(),
                    hasElevenLabsKey = elevenLabsKey.isNotBlank(),
                    onConfigChange = {
                        pipelineConfig = it
                        repository.setPipelineConfig(it)
                        refreshTick++
                    },
                    onLanguageHintsChange = {
                        languageHints = it
                        repository.setLanguageHints(it)
                    },
                    onPreserveSpokenLanguageChange = {
                        preserveSpokenLanguage = it
                        repository.setPreserveSpokenLanguage(it)
                    },
                    onRefreshGroq = {
                        modelStatus = "Refreshing Groq models..."
                        Thread {
                            val result = runCatching { GroqClient().listModels(secretStore.getApiKey(ProviderId.GROQ).orEmpty()) }
                            result.onSuccess { repository.setCachedModels(ProviderId.GROQ, it) }
                            val message = result.fold(
                                onSuccess = { "Groq models refreshed (${it.size})" },
                                onFailure = { "Groq refresh failed: ${it.message}" }
                            )
                            (context as? ComponentActivity)?.runOnUiThread {
                                result.getOrNull()?.let { groqModels = it }
                                modelStatus = message
                            }
                        }.start()
                    },
                    onRefreshOpenRouter = {
                        modelStatus = "Refreshing OpenRouter models..."
                        Thread {
                            val result = runCatching { OpenRouterClient().listModels(secretStore.getApiKey(ProviderId.OPENROUTER).orEmpty()) }
                            result.onSuccess { repository.setCachedModels(ProviderId.OPENROUTER, it) }
                            result.onSuccess {
                                refreshOpenRouterEndpointDetails(
                                    repository.getPostProcessingFavoriteIds(PostProcessingProvider.OPENROUTER) +
                                        pipelineConfig.openRouterPostProcessingModel
                                )
                            }
                            val message = result.fold(
                                onSuccess = { "OpenRouter models refreshed (${it.size})" },
                                onFailure = { "OpenRouter refresh failed: ${it.message}" }
                            )
                            (context as? ComponentActivity)?.runOnUiThread {
                                result.getOrNull()?.let { openRouterModels = it }
                                modelStatus = message
                            }
                        }.start()
                    },
                    onRefreshOpenRouterAudio = {
                        modelStatus = "Refreshing OpenRouter audio models..."
                        Thread {
                            val result = runCatching { OpenRouterClient().listAudioModels(secretStore.getApiKey(ProviderId.OPENROUTER).orEmpty()) }
                            result.onSuccess { repository.setCachedOpenRouterAudioModels(it) }
                            result.onSuccess {
                                refreshOpenRouterEndpointDetails(
                                    repository.getOpenRouterAudioFavoriteIds() +
                                        pipelineConfig.openRouterAudioTranscriptionModel +
                                        pipelineConfig.openRouterAudioDirectModel
                                )
                            }
                            val message = result.fold(
                                onSuccess = { "OpenRouter audio models refreshed (${it.size})" },
                                onFailure = { "OpenRouter audio refresh failed: ${it.message}" }
                            )
                            (context as? ComponentActivity)?.runOnUiThread {
                                result.getOrNull()?.let { openRouterAudioModels = it }
                                modelStatus = message
                            }
                        }.start()
                    },
                    cachedOpenRouterEndpointDetails = { repository.getCachedOpenRouterEndpointDetails(it) },
                    onLoadOpenRouterEndpointDetails = { modelId, callback ->
                        val cached = repository.getCachedOpenRouterEndpointDetails(modelId)
                        if (cached != null) callback(Result.success(cached))
                        Thread {
                            val result = runCatching {
                                OpenRouterClient().endpointDetails(
                                    secretStore.getApiKey(ProviderId.OPENROUTER).orEmpty(),
                                    modelId
                                )
                            }
                            result.onSuccess { repository.setCachedOpenRouterEndpointDetails(it) }
                            (context as? ComponentActivity)?.runOnUiThread { callback(result) }
                        }.start()
                    },
                    onOpenRouterProviderSortChange = {
                        openRouterProviderSort = it
                        repository.setOpenRouterProviderSort(it)
                    }
                )
                STYLE_TAB_INDEX -> StyleScreen(repository = repository)
                DICTIONARY_TAB_INDEX -> DictionaryScreen(repository = repository, config = pipelineConfig)
            }
        }
    }

    availableRelease?.let { release ->
        UpdateAvailableDialog(
            release = release,
            onOpen = {
                availableRelease = null
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)))
            },
            onSkip = {
                repository.dismissRelease(release.tagName)
                availableRelease = null
            },
            onDismiss = {
                availableRelease = null
            }
        )
    }
}

private fun installedVersionName(context: Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

private fun isAppDebuggable(context: Context): Boolean =
    context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

@Composable
private fun UpdateAvailableDialog(
    release: GitHubRelease,
    onOpen: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update available") },
        text = {
            Text("VoiceSlip ${release.tagName} is available on GitHub.")
        },
        confirmButton = {
            Button(onClick = onOpen) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open release")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip) {
                    Text("Skip this version")
                }
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}

private fun initialSetupStatus(
    context: Context,
    repository: VoiceSlipRepository,
    secretStore: SecretStore
): SetupStatus =
    currentSetupStatus(
        context = context,
        config = repository.getPipelineConfig(),
        keys = mapOf(
            ProviderId.MISTRAL to secretStore.getApiKey(ProviderId.MISTRAL).orEmpty(),
            ProviderId.GROQ to secretStore.getApiKey(ProviderId.GROQ).orEmpty(),
            ProviderId.OPENROUTER to secretStore.getApiKey(ProviderId.OPENROUTER).orEmpty(),
            ProviderId.ELEVENLABS to secretStore.getApiKey(ProviderId.ELEVENLABS).orEmpty()
        )
    )

@Composable
private fun SetupScreen(
    mistralKey: String,
    groqKey: String,
    openRouterKey: String,
    elevenLabsKey: String,
    appEnabled: Boolean,
    haptics: Boolean,
    bubbleSizeDp: Int,
    bubbleOpacityPercent: Int,
    setupStatus: SetupStatus,
    updateStatus: String?,
    showUpdatePreview: Boolean,
    onProviderKeyChange: (ProviderId, String) -> Unit,
    onAppEnabledChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBubbleSizeChange: (Int) -> Unit,
    onBubbleOpacityChange: (Int) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestNotifications: () -> Unit,
    onCheckUpdates: () -> Unit,
    onPreviewUpdate: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            if (!setupStatus.ready) {
                SetupWarning(setupStatus.missingItems)
                Spacer(Modifier.height(12.dp))
            }
            SectionTitle("Interaction")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("VoiceSlip bubble", fontWeight = FontWeight.SemiBold)
                            Text("Turns the floating dictation bubble on or off.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SettingsSwitch(checked = appEnabled, onCheckedChange = onAppEnabledChange)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Short haptics", fontWeight = FontWeight.SemiBold)
                            Text("Off by default. Vibrates briefly on record, cancel, and submit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SettingsSwitch(checked = haptics, onCheckedChange = onHapticsChange)
                    }
                    BubblePreview(sizeDp = bubbleSizeDp, opacityPercent = bubbleOpacityPercent)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingHeader("Bubble size", "${bubbleSizeDp}dp")
                        Slider(
                            value = bubbleSizeDp.toFloat(),
                            onValueChange = { onBubbleSizeChange(it.roundToInt()) },
                            valueRange = BUBBLE_SIZE_MIN_DP.toFloat()..BUBBLE_SIZE_MAX_DP.toFloat(),
                            steps = BUBBLE_SIZE_MAX_DP - BUBBLE_SIZE_MIN_DP - 1
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingHeader("Bubble opacity", "$bubbleOpacityPercent%")
                        Slider(
                            value = bubbleOpacityPercent.toFloat(),
                            onValueChange = { onBubbleOpacityChange(it.roundToInt()) },
                            valueRange = BUBBLE_OPACITY_MIN_PERCENT.toFloat()..BUBBLE_OPACITY_MAX_PERCENT.toFloat(),
                            steps = ((BUBBLE_OPACITY_MAX_PERCENT - BUBBLE_OPACITY_MIN_PERCENT) / 5) - 1
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("Permissions")
            PermissionRow(
                title = "Accessibility service",
                subtitle = "Detects editable fields and inserts text.",
                granted = setupStatus.accessibility,
                action = "Open",
                onClick = onOpenAccessibility
            )
            PermissionRow(
                title = "Draw over apps",
                subtitle = "Shows the floating dictation bubble.",
                granted = setupStatus.overlay,
                action = "Open",
                onClick = onOpenOverlay
            )
            PermissionRow(
                title = "Microphone",
                subtitle = "Records dictation audio.",
                granted = setupStatus.microphone,
                action = "Allow",
                onClick = onRequestMic
            )
            if (Build.VERSION.SDK_INT >= 33) {
                PermissionRow(
                    title = "Notifications",
                    subtitle = "Useful for long-running recording status.",
                    granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
                    action = "Allow",
                    onClick = onRequestNotifications
                )
            }
        }

        item {
            SectionTitle("Updates")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("GitHub releases", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Checks stable releases and opens GitHub when a newer version is available.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onCheckUpdates) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Check now")
                        }
                        if (showUpdatePreview) {
                            OutlinedButton(onClick = onPreviewUpdate) {
                                Text("Preview prompt")
                            }
                        }
                    }
                    updateStatus?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            SectionTitle("API keys")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProviderKeyField(
                        label = "Mistral API key",
                        value = mistralKey,
                        linkLabel = "Open Mistral API keys",
                        linkUrl = "https://console.mistral.ai/api-keys/",
                        onChange = { onProviderKeyChange(ProviderId.MISTRAL, it) }
                    )
                    ProviderKeyField(
                        label = "Groq API key",
                        value = groqKey,
                        linkLabel = "Open Groq API keys",
                        linkUrl = "https://console.groq.com/keys",
                        onChange = { onProviderKeyChange(ProviderId.GROQ, it) }
                    )
                    ProviderKeyField(
                        label = "OpenRouter API key",
                        value = openRouterKey,
                        linkLabel = "Open OpenRouter API keys",
                        linkUrl = "https://openrouter.ai/settings/keys",
                        onChange = { onProviderKeyChange(ProviderId.OPENROUTER, it) }
                    )
                    ProviderKeyField(
                        label = "ElevenLabs API key",
                        value = elevenLabsKey,
                        linkLabel = "Open ElevenLabs API keys",
                        linkUrl = "https://elevenlabs.io/app/settings/api-keys",
                        onChange = { onProviderKeyChange(ProviderId.ELEVENLABS, it) }
                    )
                    Text(
                        "Keys are stored only on this device using Android Keystore-backed encryption. Recording is blocked only when the selected pipeline is missing a required key.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

    }
}

@Composable
private fun SettingsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFFE8F0E3),
            checkedTrackColor = Color(0xFF314A3F),
            uncheckedThumbColor = Color(0xFFE8F0E3),
            uncheckedTrackColor = Color(0xFF26342D)
        )
    )
}

@Composable
private fun BubblePreview(sizeDp: Int, opacityPercent: Int) {
    val innerSizeDp = (sizeDp - 8).coerceAtLeast(1)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .alpha(opacityPercent / 100f)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFC2B8CD), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((innerSizeDp * 0.07f).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0.21f, 0.39f, 0.29f, 0.5f, 0.18f).forEach { heightRatio ->
                        Box(
                            Modifier
                                .width((innerSizeDp * 0.07f).dp)
                                .height((innerSizeDp * heightRatio).dp)
                                .background(Color(0xFF1C1820), RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingHeader(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProviderKeyField(
    label: String,
    value: String,
    linkLabel: String,
    linkUrl: String,
    onChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        TextButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl)))
            },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(linkLabel)
        }
    }
}

@Composable
private fun ModelsScreen(
    config: PipelineConfig,
    repository: VoiceSlipRepository,
    languageHints: String,
    preserveSpokenLanguage: Boolean,
    groqModels: List<ModelOption>,
    openRouterModels: List<ModelOption>,
    openRouterAudioModels: List<ModelOption>,
    openRouterProviderSort: OpenRouterProviderSort,
    modelStatus: String?,
    hasMistralKey: Boolean,
    hasGroqKey: Boolean,
    hasOpenRouterKey: Boolean,
    hasElevenLabsKey: Boolean,
    onConfigChange: (PipelineConfig) -> Unit,
    onLanguageHintsChange: (String) -> Unit,
    onPreserveSpokenLanguageChange: (Boolean) -> Unit,
    onRefreshGroq: () -> Unit,
    onRefreshOpenRouter: () -> Unit,
    onRefreshOpenRouterAudio: () -> Unit,
    cachedOpenRouterEndpointDetails: (String) -> OpenRouterEndpointDetails?,
    onLoadOpenRouterEndpointDetails: (String, (Result<OpenRouterEndpointDetails>) -> Unit) -> Unit,
    onOpenRouterProviderSortChange: (OpenRouterProviderSort) -> Unit
) {
    var route by remember { mutableStateOf<ModelsRoute>(ModelsRoute.Main) }
    var favoritesVersion by remember { mutableIntStateOf(0) }
    var showPreview by remember { mutableStateOf(false) }
    val mainListState = rememberLazyListState()
    val audioFavoriteIds = remember(openRouterAudioModels, favoritesVersion) { repository.getOpenRouterAudioFavoriteIds() }
    val groqPostFavoriteIds = remember(groqModels, favoritesVersion) { repository.getPostProcessingFavoriteIds(PostProcessingProvider.GROQ) }
    val openRouterPostFavoriteIds = remember(openRouterModels, favoritesVersion) { repository.getPostProcessingFavoriteIds(PostProcessingProvider.OPENROUTER) }
    fun refreshFavorites() { favoritesVersion++ }

    when (val currentRoute = route) {
        ModelsRoute.Main -> ModelsMainScreen(
            config = config,
            repository = repository,
            languageHints = languageHints,
            preserveSpokenLanguage = preserveSpokenLanguage,
            openRouterModels = openRouterModels,
            openRouterAudioModels = openRouterAudioModels,
            modelStatus = modelStatus,
            hasMistralKey = hasMistralKey,
            hasGroqKey = hasGroqKey,
            hasOpenRouterKey = hasOpenRouterKey,
            hasElevenLabsKey = hasElevenLabsKey,
            listState = mainListState,
            onConfigChange = onConfigChange,
            onLanguageHintsChange = onLanguageHintsChange,
            onPreserveSpokenLanguageChange = onPreserveSpokenLanguageChange,
            onPreview = { showPreview = true },
            onManageAudioModel = { route = ModelsRoute.AudioModel(it) },
            onManagePostProcessing = { route = ModelsRoute.PostProcessing }
        )
        is ModelsRoute.AudioModel -> AudioModelPickerScreen(
            role = currentRoute.role,
            config = config,
            models = openRouterAudioModels,
            favoriteIds = audioFavoriteIds,
            hasMistralKey = hasMistralKey,
            hasGroqKey = hasGroqKey,
            hasOpenRouterKey = hasOpenRouterKey,
            hasElevenLabsKey = hasElevenLabsKey,
            modelStatus = modelStatus,
            openRouterProviderSort = openRouterProviderSort,
            onBack = { route = ModelsRoute.Main },
            onRefresh = onRefreshOpenRouterAudio,
            cachedOpenRouterEndpointDetails = cachedOpenRouterEndpointDetails,
            onLoadOpenRouterEndpointDetails = onLoadOpenRouterEndpointDetails,
            onOpenRouterProviderSortChange = onOpenRouterProviderSortChange,
            onConfigChange = onConfigChange,
            onToggleFavorite = {
                repository.toggleOpenRouterAudioFavorite(it)
                refreshFavorites()
            }
        )
        ModelsRoute.PostProcessing -> PostProcessingPickerScreen(
            config = config,
            groqModels = groqModels,
            openRouterModels = openRouterModels,
            groqFavoriteIds = groqPostFavoriteIds,
            openRouterFavoriteIds = openRouterPostFavoriteIds,
            hasGroqKey = hasGroqKey,
            hasOpenRouterKey = hasOpenRouterKey,
            openRouterProviderSort = openRouterProviderSort,
            modelStatus = modelStatus,
            onBack = { route = ModelsRoute.Main },
            onConfigChange = onConfigChange,
            onRefreshGroq = onRefreshGroq,
            onRefreshOpenRouter = onRefreshOpenRouter,
            cachedOpenRouterEndpointDetails = cachedOpenRouterEndpointDetails,
            onLoadOpenRouterEndpointDetails = onLoadOpenRouterEndpointDetails,
            onOpenRouterProviderSortChange = onOpenRouterProviderSortChange,
            onToggleFavorite = { provider, modelId ->
                repository.togglePostProcessingFavorite(provider, modelId)
                refreshFavorites()
            }
        )
    }

    if (showPreview) {
        PipelinePreviewDialog(repository = repository, config = config, onDismiss = { showPreview = false })
    }
}

@Composable
private fun ModelsMainScreen(
    config: PipelineConfig,
    repository: VoiceSlipRepository,
    languageHints: String,
    preserveSpokenLanguage: Boolean,
    openRouterModels: List<ModelOption>,
    openRouterAudioModels: List<ModelOption>,
    modelStatus: String?,
    hasMistralKey: Boolean,
    hasGroqKey: Boolean,
    hasOpenRouterKey: Boolean,
    hasElevenLabsKey: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onConfigChange: (PipelineConfig) -> Unit,
    onLanguageHintsChange: (String) -> Unit,
    onPreserveSpokenLanguageChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onManageAudioModel: (AudioModelPickerRole) -> Unit,
    onManagePostProcessing: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Models")
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SettingsCard {
                    Text("Pipeline mode", fontWeight = FontWeight.SemiBold)
                    ChoiceColumn(PipelineMode.entries.map { it.label }, config.mode.label) { label ->
                        val mode = PipelineMode.entries.first { it.label == label }
                        onConfigChange(config.copy(mode = mode))
                    }
                    Text(
                        pipelineModeExplanation(config.mode),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (config.mode != PipelineMode.AUDIO_DIRECT) {
                item {
                    SettingsCard(
                        modifier = Modifier.clickable { onManageAudioModel(AudioModelPickerRole.TRANSCRIPTION) }
                    ) {
                        Text("Transcription Model", fontWeight = FontWeight.SemiBold)
                        Text(transcriptionModelSummary(config, openRouterAudioModels), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        transcriptionMissingKeySummary(config, hasMistralKey, hasGroqKey, hasOpenRouterKey, hasElevenLabsKey)?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(
                            onClick = { onManageAudioModel(AudioModelPickerRole.TRANSCRIPTION) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Choose model") }
                    }
                }
            } else {
                item {
                    SettingsCard(
                        modifier = Modifier.clickable { onManageAudioModel(AudioModelPickerRole.AUDIO_DIRECT) }
                    ) {
                        Text("Audio Direct Model", fontWeight = FontWeight.SemiBold)
                        Text(audioDirectModelSummary(config, openRouterAudioModels), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        audioDirectMissingKeySummary(config, hasMistralKey, hasOpenRouterKey)?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(
                            onClick = { onManageAudioModel(AudioModelPickerRole.AUDIO_DIRECT) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Choose model") }
                    }
                }
            }
            if (config.mode == PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING) {
                item {
                    SettingsCard(
                        modifier = Modifier.clickable { onManagePostProcessing() }
                    ) {
                        Text("Post-Processing Model", fontWeight = FontWeight.SemiBold)
                        Text(postProcessingModelSummary(config, openRouterModels), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        postProcessingMissingKeySummary(config, hasGroqKey, hasOpenRouterKey)?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(
                            onClick = onManagePostProcessing,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Choose model") }
                        modelStatus?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            item {
                DictionaryDuringTranscriptionCard(repository = repository, config = config)
            }
            item {
                LanguagePreservationCard(
                    languageHints = languageHints,
                    preserveSpokenLanguage = preserveSpokenLanguage,
                    onLanguageHintsChange = onLanguageHintsChange,
                    onPreserveSpokenLanguageChange = onPreserveSpokenLanguageChange
                )
            }
            item {
                ActivePipelineCard(repository, config)
            }
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pipeline Preview", fontWeight = FontWeight.SemiBold)
                            Text("Shows providers, exact prompts, dictionary behavior, and insertion fallback.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = onPreview) { Text("Open") }
                    }
                }
            }
        }
    }
}

private sealed class ModelsRoute {
    object Main : ModelsRoute()
    data class AudioModel(val role: AudioModelPickerRole) : ModelsRoute()
    object PostProcessing : ModelsRoute()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AudioModelPickerScreen(
    role: AudioModelPickerRole,
    config: PipelineConfig,
    models: List<ModelOption>,
    favoriteIds: List<String>,
    hasMistralKey: Boolean,
    hasGroqKey: Boolean,
    hasOpenRouterKey: Boolean,
    hasElevenLabsKey: Boolean,
    modelStatus: String?,
    openRouterProviderSort: OpenRouterProviderSort,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    cachedOpenRouterEndpointDetails: (String) -> OpenRouterEndpointDetails?,
    onLoadOpenRouterEndpointDetails: (String, (Result<OpenRouterEndpointDetails>) -> Unit) -> Unit,
    onOpenRouterProviderSortChange: (OpenRouterProviderSort) -> Unit,
    onConfigChange: (PipelineConfig) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    BackHandler { onBack() }
    var pickerState by remember { mutableStateOf(initialAudioModelPickerState(role, config)) }
    var showOpenRouterSettings by remember { mutableStateOf(false) }
    var endpointSheet by remember { mutableStateOf<EndpointSheetState?>(null) }
    var pendingSelectedScrollId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val activeProvider = pickerState.activeProvider
    val selectedModel = selectedAudioModelId(config, role, activeProvider)
    val providerOptions = when (role) {
        AudioModelPickerRole.TRANSCRIPTION -> listOf(ProviderId.MISTRAL, ProviderId.GROQ, ProviderId.ELEVENLABS, ProviderId.OPENROUTER)
        AudioModelPickerRole.AUDIO_DIRECT -> listOf(ProviderId.MISTRAL, ProviderId.OPENROUTER)
    }
    val hasKey = when (activeProvider) {
        ProviderId.MISTRAL -> hasMistralKey
        ProviderId.GROQ -> hasGroqKey
        ProviderId.OPENROUTER -> hasOpenRouterKey
        ProviderId.ELEVENLABS -> hasElevenLabsKey
    }
    val rows = remember(role, activeProvider, models, favoriteIds, selectedModel, pickerState.query) {
        if (activeProvider == ProviderId.OPENROUTER) {
            modelRows(models, favoriteIds, selectedModel, pickerState.query, fallbackProvider = "OpenRouter")
        } else {
            builtInAudioRows(role, activeProvider)
        }
    }.withOpenRouterRouteSummaries(activeProvider == ProviderId.OPENROUTER, openRouterProviderSort, cachedOpenRouterEndpointDetails)
    val selectedRow = rows.firstOrNull { isActiveAudioModel(config, role, activeProvider, it.id) }
    val listRows = rows.filterNot { selectedRow?.id == it.id }
    LaunchedEffect(selectedRow?.id, pendingSelectedScrollId, activeProvider) {
        if (selectedRow != null && pendingSelectedScrollId == selectedRow.id) {
            listState.scrollUpToSelectedIfNeeded(selectedRowScrollIndex(hasSearch = activeProvider == ProviderId.OPENROUTER))
            pendingSelectedScrollId = null
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader(audioPickerTitle(role), onBack) }
        item {
            SettingsCard {
                Text("Provider Groups", fontWeight = FontWeight.SemiBold)
                ChoiceRow(
                    options = providerOptions.map { it.label },
                    selected = activeProvider.label
                ) { label ->
                    pickerState = pickerState.switchProvider(providerOptions.first { it.label == label })
                }
                if (!hasKey) {
                    Text("Missing ${activeProvider.label} API key", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
                if (activeProvider == ProviderId.OPENROUTER) {
                    OpenRouterRoutingSummary(openRouterProviderSort, onClick = { showOpenRouterSettings = true })
                }
            }
        }
        if (activeProvider == ProviderId.OPENROUTER) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = pickerState.query,
                        onValueChange = { pickerState = pickerState.copy(query = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search models") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = onRefresh, enabled = hasOpenRouterKey) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh OpenRouter")
                            }
                        }
                    )
                    modelStatus?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (!hasOpenRouterKey) {
                        Text("Add an OpenRouter API key to refresh compatible audio models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (models.isEmpty()) {
                        Text("Refresh to load compatible OpenRouter audio models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            item { Text("No models match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        selectedRow?.let { row ->
            item(key = "selected-${row.id}") {
                PinnedSelectedModel(
                    row = row,
                    favorite = activeProvider == ProviderId.OPENROUTER && row.id in favoriteIds,
                    showFavorite = activeProvider == ProviderId.OPENROUTER,
                    onClick = {},
                    onToggleFavorite = { if (activeProvider == ProviderId.OPENROUTER) onToggleFavorite(row.id) },
                    onShowDetails = openRouterDetailsAction(
                        enabled = activeProvider == ProviderId.OPENROUTER,
                        row = row,
                        onLoadOpenRouterEndpointDetails = onLoadOpenRouterEndpointDetails,
                        setEndpointSheet = { endpointSheet = it }
                    ),
                    reasoningEffort = selectedOpenRouterAudioReasoningEffort(config, role).takeIf {
                        activeProvider == ProviderId.OPENROUTER && models.firstOrNull { model -> model.id == row.id }.supportsOpenRouterReasoning()
                    },
                    onReasoningEffortChange = { effort -> onConfigChange(pickerState.selectModel(config, row.id, effort)) }
                )
            }
        }
        items(listRows, key = { it.id }) { row ->
            ModelPickerRow(
                row = row,
                selected = false,
                savedForProvider = isSavedAudioModel(config, role, activeProvider, row.id),
                favorite = activeProvider == ProviderId.OPENROUTER && row.id in favoriteIds,
                onClick = {
                    val model = models.firstOrNull { it.id == row.id }
                    val effort = if (activeProvider == ProviderId.OPENROUTER && model.supportsOpenRouterReasoning()) {
                        OpenRouterReasoningEffort.NONE
                    } else {
                        OpenRouterReasoningEffort.NONE
                    }
                    onConfigChange(pickerState.selectModel(config, row.id, effort))
                    pendingSelectedScrollId = row.id
                },
                onToggleFavorite = { if (activeProvider == ProviderId.OPENROUTER) onToggleFavorite(row.id) },
                showFavorite = activeProvider == ProviderId.OPENROUTER,
                onShowDetails = openRouterDetailsAction(
                    enabled = activeProvider == ProviderId.OPENROUTER,
                    row = row,
                    onLoadOpenRouterEndpointDetails = onLoadOpenRouterEndpointDetails,
                    setEndpointSheet = { endpointSheet = it }
                )
            )
        }
    }
    if (showOpenRouterSettings) {
        OpenRouterSettingsSheet(
            providerSort = openRouterProviderSort,
            onProviderSortChange = onOpenRouterProviderSortChange,
            onDismiss = { showOpenRouterSettings = false }
        )
    }
    endpointSheet?.let { sheet ->
        OpenRouterEndpointDetailsSheet(sheet, openRouterProviderSort, onDismiss = { endpointSheet = null })
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PostProcessingPickerScreen(
    config: PipelineConfig,
    groqModels: List<ModelOption>,
    openRouterModels: List<ModelOption>,
    groqFavoriteIds: List<String>,
    openRouterFavoriteIds: List<String>,
    hasGroqKey: Boolean,
    hasOpenRouterKey: Boolean,
    openRouterProviderSort: OpenRouterProviderSort,
    modelStatus: String?,
    onBack: () -> Unit,
    onConfigChange: (PipelineConfig) -> Unit,
    onRefreshGroq: () -> Unit,
    onRefreshOpenRouter: () -> Unit,
    cachedOpenRouterEndpointDetails: (String) -> OpenRouterEndpointDetails?,
    onLoadOpenRouterEndpointDetails: (String, (Result<OpenRouterEndpointDetails>) -> Unit) -> Unit,
    onOpenRouterProviderSortChange: (OpenRouterProviderSort) -> Unit,
    onToggleFavorite: (PostProcessingProvider, String) -> Unit
) {
    BackHandler { onBack() }
    var pickerState by remember { mutableStateOf(initialPostProcessingPickerState(config)) }
    var showOpenRouterSettings by remember { mutableStateOf(false) }
    var endpointSheet by remember { mutableStateOf<EndpointSheetState?>(null) }
    var pendingSelectedScrollId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val activeProvider = pickerState.activeProvider
    val models = when (activeProvider) {
        PostProcessingProvider.GROQ -> groqModels
        PostProcessingProvider.OPENROUTER -> openRouterModels
        PostProcessingProvider.NONE -> emptyList()
    }
    val favoriteIds = when (activeProvider) {
        PostProcessingProvider.GROQ -> groqFavoriteIds
        PostProcessingProvider.OPENROUTER -> openRouterFavoriteIds
        PostProcessingProvider.NONE -> emptyList()
    }
    val selectedModel = selectedPostProcessingModel(config, activeProvider)
    val hasKey = when (activeProvider) {
        PostProcessingProvider.GROQ -> hasGroqKey
        PostProcessingProvider.OPENROUTER -> hasOpenRouterKey
        PostProcessingProvider.NONE -> true
    }
    val rows = remember(models, favoriteIds, selectedModel, pickerState.query, activeProvider) {
        modelRows(models, favoriteIds, selectedModel, pickerState.query, fallbackProvider = activeProvider.label)
    }.withOpenRouterRouteSummaries(activeProvider == PostProcessingProvider.OPENROUTER, openRouterProviderSort, cachedOpenRouterEndpointDetails)
    val selectedRow = rows.firstOrNull { isActivePostProcessingModel(config, activeProvider, it.id) }
    val listRows = rows.filterNot { selectedRow?.id == it.id }
    LaunchedEffect(selectedRow?.id, pendingSelectedScrollId, activeProvider) {
        if (selectedRow != null && pendingSelectedScrollId == selectedRow.id) {
            listState.scrollUpToSelectedIfNeeded(selectedRowScrollIndex(hasSearch = true))
            pendingSelectedScrollId = null
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Choose post-processing model", onBack) }
        item {
            SettingsCard {
                Text("Provider Groups", fontWeight = FontWeight.SemiBold)
                ChoiceRow(
                    options = listOf(PostProcessingProvider.GROQ.label, PostProcessingProvider.OPENROUTER.label),
                    selected = activeProvider.label
                ) { label ->
                    pickerState = pickerState.switchProvider(PostProcessingProvider.entries.first { it.label == label })
                }
                if (!hasKey) {
                    Text("Missing ${activeProvider.label} API key", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
                if (activeProvider == PostProcessingProvider.OPENROUTER) {
                    OpenRouterRoutingSummary(openRouterProviderSort, onClick = { showOpenRouterSettings = true })
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = pickerState.query,
                    onValueChange = { pickerState = pickerState.copy(query = it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search models") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = when (activeProvider) {
                                PostProcessingProvider.GROQ -> onRefreshGroq
                                PostProcessingProvider.OPENROUTER -> onRefreshOpenRouter
                                PostProcessingProvider.NONE -> onRefreshGroq
                            },
                            enabled = hasKey
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh ${activeProvider.label}")
                        }
                    }
                )
                modelStatus?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (!hasKey) {
                    Text("Add a ${activeProvider.label} API key to refresh models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (models.isEmpty()) {
                    Text("Refresh to load ${activeProvider.label} models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (rows.isEmpty()) {
            item { Text("No models match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        selectedRow?.let { row ->
            item(key = "selected-${row.id}") {
                PinnedSelectedModel(
                    row = row,
                    favorite = row.id in favoriteIds,
                    showFavorite = true,
                    onClick = {},
                    onToggleFavorite = { onToggleFavorite(activeProvider, row.id) },
                    onShowDetails = openRouterDetailsAction(
                        enabled = activeProvider == PostProcessingProvider.OPENROUTER,
                        row = row,
                        onLoadOpenRouterEndpointDetails = onLoadOpenRouterEndpointDetails,
                        setEndpointSheet = { endpointSheet = it }
                    ),
                    reasoningEffort = config.openRouterPostProcessingReasoningEffort.takeIf {
                        activeProvider == PostProcessingProvider.OPENROUTER && models.firstOrNull { model -> model.id == row.id }.supportsOpenRouterReasoning()
                    },
                    onReasoningEffortChange = { effort -> onConfigChange(pickerState.selectModel(config, row.id, effort)) }
                )
            }
        }
        items(listRows, key = { it.id }) { row ->
            ModelPickerRow(
                row = row,
                selected = false,
                savedForProvider = isSavedPostProcessingModel(config, activeProvider, row.id),
                favorite = row.id in favoriteIds,
                onClick = {
                    val model = models.firstOrNull { it.id == row.id }
                    val effort = if (activeProvider == PostProcessingProvider.OPENROUTER && model.supportsOpenRouterReasoning()) {
                        OpenRouterReasoningEffort.NONE
                    } else {
                        OpenRouterReasoningEffort.NONE
                    }
                    onConfigChange(pickerState.selectModel(config, row.id, effort))
                    pendingSelectedScrollId = row.id
                },
                onToggleFavorite = { onToggleFavorite(activeProvider, row.id) },
                onShowDetails = openRouterDetailsAction(
                    enabled = activeProvider == PostProcessingProvider.OPENROUTER,
                    row = row,
                    onLoadOpenRouterEndpointDetails = onLoadOpenRouterEndpointDetails,
                    setEndpointSheet = { endpointSheet = it }
                )
            )
        }
    }
    if (showOpenRouterSettings) {
        OpenRouterSettingsSheet(
            providerSort = openRouterProviderSort,
            onProviderSortChange = onOpenRouterProviderSortChange,
            onDismiss = { showOpenRouterSettings = false }
        )
    }
    endpointSheet?.let { sheet ->
        OpenRouterEndpointDetailsSheet(sheet, openRouterProviderSort, onDismiss = { endpointSheet = null })
    }
}

@Composable
private fun ModelPickerRow(
    row: ModelDisplayRow,
    selected: Boolean,
    savedForProvider: Boolean = false,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    showFavorite: Boolean = true,
    onShowDetails: (() -> Unit)? = null
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (savedForProvider) {
                    Modifier.drawWithContent {
                        drawContent()
                        val strokeWidth = 1.5.dp.toPx()
                        drawRoundRect(
                            color = outlineColor,
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(row.name, fontWeight = FontWeight.SemiBold, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(modelRowSubtitle(row), color = if (selected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (savedForProvider) {
                    Text("Last selected for ${row.provider}; not active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showFavorite) {
                onShowDetails?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Model details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (favorite) "Remove favorite" else "Add favorite",
                        tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedSelectedModel(
    row: ModelDisplayRow,
    favorite: Boolean,
    showFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowDetails: (() -> Unit)?,
    reasoningEffort: OpenRouterReasoningEffort?,
    onReasoningEffortChange: (OpenRouterReasoningEffort) -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(row.name, fontWeight = FontWeight.SemiBold, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(modelRowSubtitle(row.withReasoningDetail(reasoningEffort)), color = contentColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (showFavorite) {
                        onShowDetails?.let {
                            IconButton(onClick = it) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Model details",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (favorite) "Remove favorite" else "Add favorite",
                                tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                reasoningEffort?.let { effort ->
                    InlineReasoningPanel(selected = effort, onSelect = onReasoningEffortChange)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineReasoningPanel(
    selected: OpenRouterReasoningEffort,
    onSelect: (OpenRouterReasoningEffort) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Reasoning effort", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            reasoningEffortSelectionOrder.forEach { effort ->
                CompactReasoningChip(
                    selected = selected == effort,
                    label = if (effort == OpenRouterReasoningEffort.NONE) "None · Recommended" else effort.label,
                    icon = effort.icon(),
                    onClick = { onSelect(effort) }
                )
            }
        }
    }
}

private fun ModelDisplayRow.withReasoningDetail(reasoningEffort: OpenRouterReasoningEffort?): ModelDisplayRow =
    if (reasoningEffort == null) {
        this
    } else {
        copy(detail = listOf(detail, "🧠 ${reasoningEffort.label}").filter { it.isNotBlank() }.joinToString(" · "))
    }

private fun openRouterDetailsAction(
    enabled: Boolean,
    row: ModelDisplayRow,
    onLoadOpenRouterEndpointDetails: (String, (Result<OpenRouterEndpointDetails>) -> Unit) -> Unit,
    setEndpointSheet: (EndpointSheetState) -> Unit
): (() -> Unit)? = if (enabled) {
    {
        setEndpointSheet(EndpointSheetState(row.name, row.id, null, "Loading endpoint details..."))
        onLoadOpenRouterEndpointDetails(row.id) { result ->
            setEndpointSheet(
                result.fold(
                    onSuccess = { EndpointSheetState(row.name, row.id, it, null) },
                    onFailure = { EndpointSheetState(row.name, row.id, null, it.message ?: "Endpoint details failed.") }
                )
            )
        }
    }
} else {
    null
}

private fun selectedRowScrollIndex(hasSearch: Boolean): Int =
    if (hasSearch) 3 else 2

@Composable
private fun OpenRouterRoutingSummary(sort: OpenRouterProviderSort, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(sort.icon(), contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("OpenRouter routing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (sort == OpenRouterProviderSort.THROUGHPUT) "${sort.label} · Recommended" else sort.label,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = "OpenRouter settings", modifier = Modifier.size(18.dp))
            }
        }
    }
}

private suspend fun androidx.compose.foundation.lazy.LazyListState.scrollUpToSelectedIfNeeded(index: Int) {
    val visibleSelected = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (visibleSelected == null) {
        if (firstVisibleItemIndex > index) animateScrollToItem(index)
        return
    }
    if (visibleSelected.offset < layoutInfo.viewportStartOffset) {
        animateScrollToItem(index)
    }
}

private fun modelRowSubtitle(row: ModelDisplayRow): String {
    val availability = if (row.isAvailable) "" else " · Unavailable in latest refresh"
    return "${row.detail}$availability"
}

private fun List<ModelDisplayRow>.withOpenRouterRouteSummaries(
    enabled: Boolean,
    providerSort: OpenRouterProviderSort,
    cachedDetails: (String) -> OpenRouterEndpointDetails?
): List<ModelDisplayRow> {
    if (!enabled || providerSort == OpenRouterProviderSort.DEFAULT) return this
    return map { row ->
        val summary = cachedDetails(row.id)?.predictedRouteSummary(providerSort)
        if (summary == null) row else row.copy(detail = summary)
    }
}

private fun OpenRouterEndpointDetails.predictedRouteSummary(providerSort: OpenRouterProviderSort): String? =
    endpoints.sortedFor(providerSort).firstOrNull()?.let { endpoint ->
        listOf(
            endpoint.providerName.ifBlank { endpoint.name },
            endpoint.pricePair(showUnit = false),
            endpoint.throughput.p50?.let { keepTogether("${formatNumber(it)} t/s") } ?: keepTogether("speed n/a"),
            endpoint.latency.p50?.let { keepTogether("${formatLatencyMillis(it)} TTFT") } ?: keepTogether("TTFT n/a")
        ).joinToString(" · ")
    }

private data class EndpointSheetState(
    val modelName: String,
    val modelId: String,
    val details: OpenRouterEndpointDetails?,
    val status: String?
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OpenRouterEndpointDetailsSheet(
    state: EndpointSheetState,
    providerSort: OpenRouterProviderSort,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val endpoints = state.details?.endpoints.orEmpty().sortedFor(providerSort)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(state.modelName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(state.modelId, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Routing: ${providerSort.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (endpoints.isNotEmpty()) {
                    Text("TTFT is p50 time to first token. Throughput is output tokens per second.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.status?.let { status ->
                item { Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (endpoints.isNotEmpty()) {
                item {
                    Text(
                        if (providerSort == OpenRouterProviderSort.DEFAULT) "Endpoint details" else "Endpoint candidates",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(endpoints) { endpoint ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(endpoint.providerName.ifBlank { endpoint.name }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (endpoint.tag.isNotBlank()) {
                                Text(endpoint.tag, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MetricChip(Icons.Filled.AttachMoney, endpoint.pricePair(showUnit = true))
                                MetricChip(Icons.Filled.Speed, endpoint.throughput.p50?.let { "${formatNumber(it)} t/s" } ?: "speed n/a")
                                MetricChip(Icons.Filled.Timer, endpoint.latency.p50?.let { "${formatLatencyMillis(it)} TTFT" } ?: "TTFT n/a")
                                MetricChip(Icons.Filled.Percent, endpoint.uptimeLast30m?.let { "uptime ${formatNumber(it)}%" } ?: "uptime n/a")
                            }
                        }
                    }
                }
            } else if (state.details != null) {
                item {
                    Text(
                        "No endpoint candidates available yet. This model can still be selected; OpenRouter will choose a route at request time.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun List<com.imdinkie.voiceslip.data.OpenRouterEndpointOption>.sortedFor(sort: OpenRouterProviderSort): List<com.imdinkie.voiceslip.data.OpenRouterEndpointOption> =
    when (sort) {
        OpenRouterProviderSort.PRICE -> sortedBy { it.totalPricePerMillion() ?: Double.MAX_VALUE }
        OpenRouterProviderSort.THROUGHPUT -> sortedWith(compareByDescending<com.imdinkie.voiceslip.data.OpenRouterEndpointOption> { it.throughput.p50 ?: -1.0 })
        OpenRouterProviderSort.LATENCY -> sortedBy { it.latency.p50 ?: Double.MAX_VALUE }
        OpenRouterProviderSort.DEFAULT -> sortedBy { it.totalPricePerMillion() ?: Double.MAX_VALUE }
    }

private fun com.imdinkie.voiceslip.data.OpenRouterEndpointOption.totalPricePerMillion(): Double? {
    val input = promptPricePerMillion ?: return null
    val output = completionPricePerMillion ?: return null
    return input + output
}

@Composable
private fun MetricChip(icon: ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(keepTogether(label), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

private fun com.imdinkie.voiceslip.data.OpenRouterEndpointOption.pricePair(showUnit: Boolean): String {
    val input = promptPricePerMillion
    val output = completionPricePerMillion
    if (input == null || output == null) return "price n/a"
    val unit = if (showUnit) " per MTok" else ""
    return "${formatMoney(input)}/${formatMoney(output)}$unit"
}

private fun formatMoney(value: Double): String =
    if (value == 0.0) "\$0" else "\$" + if (value < 10) "%.2f".format(value) else "%.0f".format(value)

private fun formatNumber(value: Double): String =
    if (value < 10) "%.2f".format(value) else "%.0f".format(value)

private fun formatLatencyMillis(value: Double): String {
    val millis = if (value < 100) value * 1000.0 else value
    return if (millis < 1000) "${"%.0f".format(millis)} ms" else "${"%.1f".format(millis / 1000.0)} s"
}

private fun keepTogether(value: String): String = value.replace(" ", "\u2060\u00A0\u2060").replace("/", "\u2060/\u2060")

private fun ModelOption?.supportsOpenRouterReasoning(): Boolean =
    this?.supportedParameters.orEmpty().any { it.equals("reasoning", ignoreCase = true) }

private fun selectedOpenRouterAudioReasoningEffort(config: PipelineConfig, role: AudioModelPickerRole): OpenRouterReasoningEffort =
    when (role) {
        AudioModelPickerRole.TRANSCRIPTION -> config.openRouterAudioTranscriptionReasoningEffort
        AudioModelPickerRole.AUDIO_DIRECT -> config.openRouterAudioDirectReasoningEffort
    }

private val reasoningEffortSelectionOrder = listOf(
    OpenRouterReasoningEffort.NONE,
    OpenRouterReasoningEffort.MINIMAL,
    OpenRouterReasoningEffort.LOW,
    OpenRouterReasoningEffort.MEDIUM,
    OpenRouterReasoningEffort.HIGH,
    OpenRouterReasoningEffort.XHIGH,
    OpenRouterReasoningEffort.AUTO
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OpenRouterSettingsSheet(
    providerSort: OpenRouterProviderSort,
    onProviderSortChange: (OpenRouterProviderSort) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("OpenRouter settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Applies to every OpenRouter request.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingHeader("Provider routing", providerSort.label)
                Text(
                    "Choose how OpenRouter should prefer eligible providers. Default keeps OpenRouter's normal routing.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OpenRouterProviderSort.entries.forEach { sort ->
                        CompactChoiceChip(
                            selected = providerSort == sort,
                            label = if (sort == OpenRouterProviderSort.THROUGHPUT) "${sort.label} · Recommended" else sort.label,
                            icon = sort.icon(),
                            onClick = { onProviderSortChange(sort) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompactChoiceChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.height(36.dp).widthIn(min = 92.dp),
        label = { Text(label, maxLines = 1) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    )
}

@Composable
private fun CompactReasoningChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.height(30.dp).widthIn(min = 66.dp),
        label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
        }
    )
}

private fun OpenRouterProviderSort.icon(): ImageVector = when (this) {
    OpenRouterProviderSort.DEFAULT -> Icons.Filled.Tune
    OpenRouterProviderSort.PRICE -> Icons.Filled.AttachMoney
    OpenRouterProviderSort.THROUGHPUT -> Icons.Filled.Speed
    OpenRouterProviderSort.LATENCY -> Icons.Filled.Timer
}

private fun OpenRouterReasoningEffort.icon(): ImageVector = when (this) {
    OpenRouterReasoningEffort.AUTO -> Icons.Filled.AutoAwesome
    OpenRouterReasoningEffort.NONE -> Icons.Filled.Block
    OpenRouterReasoningEffort.MINIMAL,
    OpenRouterReasoningEffort.LOW -> Icons.Filled.Bolt
    OpenRouterReasoningEffort.MEDIUM -> Icons.Filled.Psychology
    OpenRouterReasoningEffort.HIGH,
    OpenRouterReasoningEffort.XHIGH -> Icons.Filled.Psychology
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            if (option == selected) Button(onClick = { onSelect(option) }) { Text(option) }
            else OutlinedButton(onClick = { onSelect(option) }) { Text(option) }
        }
    }
}

@Composable
private fun ChoiceColumn(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            if (option == selected) Button(onClick = { onSelect(option) }, modifier = Modifier.fillMaxWidth()) { Text(option) }
            else OutlinedButton(onClick = { onSelect(option) }, modifier = Modifier.fillMaxWidth()) { Text(option) }
        }
    }
}

private fun transcriptionModelSummary(config: PipelineConfig, openRouterModels: List<ModelOption>): String =
    if (config.transcriptionProvider() == ProviderId.OPENROUTER) {
        openRouterSelectionSummary(
            modelId = config.openRouterAudioTranscriptionModel,
            effort = config.openRouterAudioTranscriptionReasoningEffort,
            model = openRouterModels.firstOrNull { it.id == config.openRouterAudioTranscriptionModel }
        )
    } else {
        "${config.transcriptionProvider().label} · ${config.transcriptionModel()} · ${config.transcriptionDisplayName()}"
    }

private fun audioDirectModelSummary(config: PipelineConfig, openRouterModels: List<ModelOption>): String =
    if (config.audioDirectProvider() == ProviderId.OPENROUTER) {
        openRouterSelectionSummary(
            modelId = config.openRouterAudioDirectModel,
            effort = config.openRouterAudioDirectReasoningEffort,
            model = openRouterModels.firstOrNull { it.id == config.openRouterAudioDirectModel }
        )
    } else {
        "${config.audioDirectProvider().label} · ${config.audioDirectModel()} · ${config.audioDirectDisplayName()}"
    }

private fun openRouterSelectionSummary(
    modelId: String,
    effort: OpenRouterReasoningEffort,
    model: ModelOption?
): String {
    val displayName = model?.name?.takeIf { it.isNotBlank() && it != modelId } ?: modelId.ifBlank { "OpenRouter model" }
    val reasoning = if (model.supportsOpenRouterReasoning() || effort != OpenRouterReasoningEffort.NONE) {
        " · 🧠 ${effort.label}"
    } else {
        ""
    }
    return "OpenRouter · $displayName$reasoning"
}

private fun audioPickerTitle(role: AudioModelPickerRole): String =
    when (role) {
        AudioModelPickerRole.TRANSCRIPTION -> "Choose transcription model"
        AudioModelPickerRole.AUDIO_DIRECT -> "Choose audio direct model"
    }

private fun transcriptionMissingKeySummary(
    config: PipelineConfig,
    hasMistralKey: Boolean,
    hasGroqKey: Boolean,
    hasOpenRouterKey: Boolean,
    hasElevenLabsKey: Boolean
): String? = when (config.transcriptionProvider()) {
    ProviderId.MISTRAL -> if (hasMistralKey) null else "Missing Mistral API key for the selected transcription model."
    ProviderId.GROQ -> if (hasGroqKey) null else "Missing Groq API key for the selected transcription model."
    ProviderId.OPENROUTER -> if (hasOpenRouterKey) null else "Missing OpenRouter API key for the selected transcription model."
    ProviderId.ELEVENLABS -> if (hasElevenLabsKey) null else "Missing ElevenLabs API key for the selected transcription model."
}

private fun audioDirectMissingKeySummary(
    config: PipelineConfig,
    hasMistralKey: Boolean,
    hasOpenRouterKey: Boolean
): String? = when (config.audioDirectProvider()) {
    ProviderId.MISTRAL -> if (hasMistralKey) null else "Missing Mistral API key for the selected audio direct model."
    ProviderId.OPENROUTER -> if (hasOpenRouterKey) null else "Missing OpenRouter API key for the selected audio direct model."
    ProviderId.GROQ,
    ProviderId.ELEVENLABS -> null
}

private fun postProcessingModelSummary(config: PipelineConfig, openRouterModels: List<ModelOption>): String {
    if (config.postProcessingProvider == PostProcessingProvider.NONE || config.postProcessingModel.isBlank()) {
        return "No Post-Processing Model selected"
    }
    if (config.postProcessingProvider == PostProcessingProvider.OPENROUTER) {
        return openRouterSelectionSummary(
            modelId = config.openRouterPostProcessingModel,
            effort = config.openRouterPostProcessingReasoningEffort,
            model = openRouterModels.firstOrNull { it.id == config.openRouterPostProcessingModel }
        )
    }
    return "${config.postProcessingProvider.label} · ${config.postProcessingModel}"
}

private fun postProcessingMissingKeySummary(
    config: PipelineConfig,
    hasGroqKey: Boolean,
    hasOpenRouterKey: Boolean
): String? {
    if (config.postProcessingModel.isBlank()) return null
    return when (config.postProcessingProvider) {
        PostProcessingProvider.GROQ -> if (hasGroqKey) null else "Missing Groq API key for the selected post-processing model."
        PostProcessingProvider.OPENROUTER -> if (hasOpenRouterKey) null else "Missing OpenRouter API key for the selected post-processing model."
        PostProcessingProvider.NONE -> null
    }
}

private const val DETECTED_LANGUAGE_PLACEHOLDER = "{{detected_language}}"

private fun audioChatTranscriptionPromptPreview(languageHints: String, preserveSpokenLanguage: Boolean, dictionaryPrompt: String?): String =
    buildAudioTranscriptionPromptPreview(languageHints, preserveSpokenLanguage, dictionaryPrompt)

private fun postProcessingSystemPromptPreview(cleanupPolicy: String, preserveSpokenLanguage: Boolean, dictionarySize: Int): String =
    buildPostProcessingSystemPrompt(
        detectedLanguage = DETECTED_LANGUAGE_PLACEHOLDER,
        dictionaryTerms = if (dictionarySize > 0) listOf("{{dictionary_terms}}") else emptyList(),
        cleanupPolicy = cleanupPolicy,
        preserveSpokenLanguage = preserveSpokenLanguage
    )

private fun audioDirectPromptPreview(
    cleanupPolicy: String,
    stylePrompt: String,
    languageHints: String,
    preserveSpokenLanguage: Boolean,
    dictionarySize: Int
): String = buildAudioDirectPrompt(
    cleanupPolicy = cleanupPolicy,
    stylePrompt = stylePrompt,
    languageHints = languageHints,
    preserveSpokenLanguage = preserveSpokenLanguage,
    dictionaryTerms = if (dictionarySize > 0) listOf("{{dictionary_terms}}") else emptyList()
)

private fun transcriptionDisplayName(repository: VoiceSlipRepository, config: PipelineConfig): String {
    if (config.transcriptionEngineKind == EngineKind.BUILT_IN) return config.transcriptionEngine.displayName
    val model = config.openRouterAudioTranscriptionModel
    return repository.getCachedOpenRouterAudioModels().firstOrNull { it.id == model }?.name ?: model.ifBlank { "OpenRouter audio" }
}

private fun transcriptionEndpoint(config: PipelineConfig): String = when {
    config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO -> "/api/v1/chat/completions"
    config.transcriptionEngine.provider == ProviderId.GROQ -> "/openai/v1/audio/transcriptions"
    config.transcriptionEngine.provider == ProviderId.ELEVENLABS -> "/v1/speech-to-text"
    config.transcriptionEngine.audioChat -> "/v1/chat/completions"
    else -> "/v1/audio/transcriptions"
}

private fun openRouterAudioModelAvailable(repository: VoiceSlipRepository, modelId: String): Boolean =
    repository.getCachedOpenRouterAudioModels().any { it.id == modelId }

private fun pipelineModeExplanation(mode: PipelineMode): String {
    return when (mode) {
        PipelineMode.PURE_TRANSCRIPTION ->
            "Only turns audio into text. Audio-chat engines in this mode are prompted to return a raw transcript, with no style cleanup."
        PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING ->
            "First creates a raw transcript, then sends that text to Groq or OpenRouter for the selected style cleanup."
        PipelineMode.AUDIO_DIRECT ->
            "Sends audio and the style instruction to one Mistral audio-chat model. There may be no separate raw transcript."
    }
}

@Composable
private fun LanguagePreservationCard(
    languageHints: String,
    preserveSpokenLanguage: Boolean,
    onLanguageHintsChange: (String) -> Unit,
    onPreserveSpokenLanguageChange: (Boolean) -> Unit
) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Language Preservation", fontWeight = FontWeight.SemiBold)
                Text("Keeps output in the language you dictated.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SettingsSwitch(
                checked = preserveSpokenLanguage,
                onCheckedChange = onPreserveSpokenLanguageChange
            )
        }
        Text("Language Hints", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = languageHints,
            onValueChange = onLanguageHintsChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Comma-separated languages") },
            singleLine = true,
            enabled = preserveSpokenLanguage
        )
        if (preserveSpokenLanguage) {
            val helperText = buildLanguageHintExamples(languageHints)
                .ifBlank { "Optional. Add hints only when your dictation languages need extra guidance." }
            Text(helperText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivePipelineCard(repository: VoiceSlipRepository, config: PipelineConfig) {
    SettingsCard {
        Text("Active Pipeline", fontWeight = FontWeight.SemiBold)
        when (config.mode) {
            PipelineMode.PURE_TRANSCRIPTION -> {
                PipelineDetailRow("Mode", config.mode.label)
                PipelineDetailRow("Transcription", "${config.transcriptionProvider().label} · ${config.transcriptionModel()}")
                PipelineDetailRow("Audio", "Records ${repository.recordingFormatForConfig(config).label} automatically")
                PipelineDetailRow("Output", "Raw transcript")
            }
            PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING -> {
                PipelineDetailRow("Mode", config.mode.label)
                PipelineDetailRow("Transcription", "${config.transcriptionProvider().label} · ${config.transcriptionModel()}")
                PipelineDetailRow("Audio", "Records ${repository.recordingFormatForConfig(config).label} automatically")
                PipelineDetailRow("Cleanup", "${config.postProcessingProvider.label} · ${config.postProcessingModel.ifBlank { "(select model)" }}")
                PipelineDetailRow("Style", "Resolved at recording start")
            }
            PipelineMode.AUDIO_DIRECT -> {
                PipelineDetailRow("Mode", config.mode.label)
                PipelineDetailRow("Audio direct", "${config.audioDirectProvider().label} · ${config.audioDirectModel()}")
                PipelineDetailRow("Audio", "Records ${repository.recordingFormatForConfig(config).label} automatically")
                PipelineDetailRow("Style", "Sent with audio in one call")
            }
        }
    }
}

@Composable
private fun PipelineDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.width(112.dp), fontWeight = FontWeight.SemiBold)
        Text(value, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DictionaryDuringTranscriptionCard(repository: VoiceSlipRepository, config: PipelineConfig) {
    val entries = repository.listDictionary()
    val routingId = if (config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO) OPENROUTER_AUDIO_TRANSCRIPTION_ROUTING_ID else config.transcriptionEngine.name
    val engineName = if (config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO) "OpenRouter audio" else config.transcriptionEngine.displayName
    var routing by remember(routingId) { mutableStateOf(repository.routingForEngine(routingId)) }
    val plan = remember(routing, entries, config) {
        if (config.mode == PipelineMode.AUDIO_DIRECT) null else repository.dictionaryPlanForTranscription(config, entries.map { it.phrase })
    }
    SettingsCard {
        Text("Dictionary During Transcription", fontWeight = FontWeight.SemiBold)
        if (config.mode == PipelineMode.AUDIO_DIRECT) {
            Text(
                "Audio direct always sends all ${entries.size} Dictionary Entries as prompt spelling constraints. There is no separate switch.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val routingEnabled = routing.sendDictionaryToTranscription
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(engineName)
                    if (routingEnabled) {
                        plan?.let { Text(dictionaryDuringTranscriptionDetail(it), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                SettingsSwitch(
                    checked = routingEnabled,
                    onCheckedChange = {
                        repository.setRoutingForEngine(routingId, it)
                        routing = EngineDictionaryRoutingEntity(routingId, it)
                    }
                )
            }
            if (routingEnabled && entries.isEmpty()) {
                Text("Off. No Dictionary Entries saved, so no transcription dictionary request is sent.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Cleanup always receives all ${entries.size} Dictionary Entries.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun dictionaryDuringTranscriptionDetail(plan: com.imdinkie.voiceslip.data.DictionaryPromptPlan): String {
    if (!plan.sent) return "Off. Cleanup still receives all ${plan.totalTerms} Dictionary Entries."
    return when (plan.mechanism) {
        "ElevenLabs keyterms" -> {
            val base = "Sends ${plan.includedTerms} ElevenLabs keyterms from ${plan.totalTerms} Dictionary Entries. Adds a 20% ElevenLabs STT premium."
            if (plan.truncated && plan.limit != null) "$base Provider limit: ${plan.limit} keyterms." else base
        }
        "ElevenLabs keyterms unavailable" -> "Dictionary keyterms are only available on Scribe v2."
        "Mistral context_bias" -> {
            val base = "Sends ${plan.includedTerms} Mistral Bias Tokens derived from ${plan.totalTerms} Dictionary Entries."
            if (plan.truncated && plan.limit != null) "$base Provider limit: ${plan.limit} Bias Tokens." else base
        }
        "Groq multipart prompt" -> {
            val base = "Sends ${plan.includedTerms} of ${plan.totalTerms} Dictionary Entries through the Groq transcription prompt field."
            if (plan.truncated && plan.limit != null) "$base Prompt budget: ${plan.limit} characters." else base
        }
        "OpenRouter audio prompt spelling constraints" ->
            "Sends all ${plan.includedTerms} Dictionary Entries through the OpenRouter audio prompt."
        else ->
            "Sends all ${plan.includedTerms} Dictionary Entries as prompt spelling constraints."
    }
}

@Composable
private fun PipelinePreviewDialog(repository: VoiceSlipRepository, config: PipelineConfig, onDismiss: () -> Unit) {
    val dictionary = repository.listDictionary().map { it.phrase }
    val resolution = repository.resolveStyleForPackage(null)
    val transcriptionPlan = if (config.mode == PipelineMode.AUDIO_DIRECT) null else repository.dictionaryPlanForTranscription(config, dictionary)
    val cleanupPolicy = repository.getCleanupPolicy()
    val languageHints = repository.getLanguageHints()
    val preserveSpokenLanguage = repository.getPreserveSpokenLanguage()
    val transcriptionProvider = config.transcriptionProvider()
    val transcriptionUsesAudioPrompt = config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO || config.transcriptionEngine.audioChat
    val audioDirectProvider = config.audioDirectProvider()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pipeline Preview") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Mode", fontWeight = FontWeight.SemiBold)
                    Text(config.mode.label)
                }
                if (config.mode != PipelineMode.AUDIO_DIRECT) {
                    item {
                        Text("Step 1: Transcription", fontWeight = FontWeight.SemiBold)
                        Text("Provider: ${transcriptionProvider.label}")
                        Text("Engine: ${transcriptionDisplayName(repository, config)}")
                        Text("Model: ${config.transcriptionModel().ifBlank { "(select model)" }}")
                        if (config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO && config.openRouterAudioTranscriptionModel.isNotBlank() && !openRouterAudioModelAvailable(repository, config.openRouterAudioTranscriptionModel)) {
                            Text("Unavailable in latest refresh", color = MaterialTheme.colorScheme.error)
                        }
                        Text("Endpoint: ${transcriptionEndpoint(config)}")
                        Text("Audio: records ${repository.recordingFormatForConfig(config).label} automatically")
                        if (config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO) {
                            Text("Audio input: base64 input_audio")
                        }
                        transcriptionPlan?.let {
                            Text("Dictionary: ${it.mechanism}")
                            Text(dictionaryDuringTranscriptionDetail(it))
                            if (transcriptionUsesAudioPrompt) {
                                Text("Prompt:\n${audioChatTranscriptionPromptPreview(languageHints, preserveSpokenLanguage, it.prompt)}")
                            } else {
                                it.prompt?.let { prompt -> Text("Prompt:\n$prompt") }
                            }
                        }
                    }
                }
                if (config.mode == PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING) {
                    item {
                        Text("Step 2: Post-processing", fontWeight = FontWeight.SemiBold)
                        Text("Provider: ${config.postProcessingProvider.label}")
                        Text("Model: ${config.postProcessingModel.ifBlank { "(select model)" }}")
                        Text("Dictionary: cleanup receives all ${dictionary.size} Dictionary Entries as spelling constraints")
                        Text("Resolved style: {{style_prompt}}")
                        Text("System prompt:\n${postProcessingSystemPromptPreview(cleanupPolicy, preserveSpokenLanguage, dictionary.size)}")
                        Text("User prompt:\n${buildPostProcessingUserPrompt(resolution.stylePrompt, "{{raw_transcript}}")}")
                    }
                }
                if (config.mode == PipelineMode.AUDIO_DIRECT) {
                    item {
                        Text("Step 1: Audio direct", fontWeight = FontWeight.SemiBold)
                        Text("Provider: ${audioDirectProvider.label}")
                        Text("Model: ${config.audioDirectModel().ifBlank { "(select model)" }}")
                        if (config.audioDirectEngineKind == EngineKind.OPENROUTER_AUDIO && config.openRouterAudioDirectModel.isNotBlank() && !openRouterAudioModelAvailable(repository, config.openRouterAudioDirectModel)) {
                            Text("Unavailable in latest refresh", color = MaterialTheme.colorScheme.error)
                        }
                        Text("Endpoint: ${if (audioDirectProvider == ProviderId.OPENROUTER) "/api/v1/chat/completions" else "/v1/chat/completions"}")
                        if (audioDirectProvider == ProviderId.OPENROUTER) {
                            Text("Audio input: base64 input_audio")
                        }
                        Text("Dictionary: all ${dictionary.size} Dictionary Entries included as prompt spelling constraints")
                        Text("Resolved style: {{style_prompt}}")
                        Text("Prompt:\n${audioDirectPromptPreview(cleanupPolicy, resolution.stylePrompt, languageHints, preserveSpokenLanguage, dictionary.size)}")
                    }
                }
                item {
                    Text("Output", fontWeight = FontWeight.SemiBold)
                    Text("Final text is inserted into the focused field. If direct insertion fails, VoiceSlip copies final text to clipboard.")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun StyleScreen(repository: VoiceSlipRepository) {
    val context = LocalContext.current
    var route by remember { mutableStateOf<StyleRoute>(StyleRoute.Home) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var refreshState by remember { mutableStateOf(StyleRefreshState.from(repository.installedAppCacheState())) }
    fun reload() { refreshKey++ }
    fun refreshApps(force: Boolean) {
        val cache = repository.installedAppCacheState()
        if (!force && !cache.isEmpty && !cache.isStale) {
            refreshState = StyleRefreshState.from(cache)
            return
        }
        if (refreshState.isRefreshing) return
        refreshState = StyleRefreshState.from(cache).copy(isRefreshing = true, message = "Refreshing apps...")
        Thread {
            val result = runCatching { repository.refreshInstalledApps() }
            val updated = StyleRefreshState.from(repository.installedAppCacheState()).copy(
                isRefreshing = false,
                message = result.fold(
                    onSuccess = { "Apps refreshed" },
                    onFailure = { "Refresh failed: ${it.message}" }
                )
            )
            (context as? ComponentActivity)?.runOnUiThread {
                refreshState = updated
                reload()
            }
        }.start()
    }
    LaunchedEffect(Unit) {
        refreshApps(force = false)
    }
    when (val current = route) {
        StyleRoute.Home -> StyleHomeScreen(repository, refreshKey, refreshState, onRoute = { route = it }, onReload = ::reload)
        is StyleRoute.CategoryDetail -> CategoryDetailScreen(
            repository = repository,
            categoryId = current.categoryId,
            refreshKey = refreshKey,
            refreshState = refreshState,
            onBack = { route = StyleRoute.Home; reload() },
            onChooseStyle = { route = StyleRoute.StyleLibrary(chooseForCategoryId = current.categoryId) },
            onEditStyle = { route = StyleRoute.StyleEditor(it, StyleRoute.CategoryDetail(current.categoryId)) },
            onPickAppCategory = { route = StyleRoute.AppCategoryPicker(it, StyleRoute.CategoryDetail(current.categoryId)) },
            onRefreshApps = { refreshApps(force = true) },
            onReload = ::reload
        )
        StyleRoute.AllApps -> AllAppsScreen(
            repository = repository,
            refreshKey = refreshKey,
            refreshState = refreshState,
            onBack = { route = StyleRoute.Home; reload() },
            onPickAppCategory = { route = StyleRoute.AppCategoryPicker(it, StyleRoute.AllApps) },
            onRefreshApps = { refreshApps(force = true) },
            onReload = ::reload
        )
        is StyleRoute.StyleLibrary -> StyleLibraryScreen(
            repository = repository,
            chooseForCategoryId = current.chooseForCategoryId,
            refreshKey = refreshKey,
            onBack = { route = current.chooseForCategoryId?.let { StyleRoute.CategoryDetail(it) } ?: StyleRoute.Home; reload() },
            onEdit = { route = StyleRoute.StyleEditor(it, current) },
            onAdd = { route = StyleRoute.NewStyle(current) },
            onReload = ::reload
        )
        is StyleRoute.NewStyle -> NewStyleScreen(
            repository = repository,
            onBack = { route = current.returnTo; reload() },
            onCreated = { styleId ->
                reload()
                route = StyleRoute.StyleEditor(styleId, current.returnTo)
            }
        )
        is StyleRoute.StyleEditor -> StyleEditorScreen(
            repository = repository,
            styleId = current.styleId,
            onBack = { route = current.returnTo; reload() },
            onReload = ::reload
        )
        is StyleRoute.AppCategoryPicker -> AppCategoryPickerScreen(
            repository = repository,
            packageName = current.packageName,
            refreshKey = refreshKey,
            onBack = { route = current.returnTo; reload() },
            onReload = ::reload
        )
        StyleRoute.PromptSettings -> PromptSettingsScreen(repository, onBack = { route = StyleRoute.Home })
    }
}

private sealed class StyleRoute {
    data object Home : StyleRoute()
    data class CategoryDetail(val categoryId: String) : StyleRoute()
    data object AllApps : StyleRoute()
    data class AppCategoryPicker(val packageName: String, val returnTo: StyleRoute) : StyleRoute()
    data class StyleLibrary(val chooseForCategoryId: String? = null) : StyleRoute()
    data class NewStyle(val returnTo: StyleRoute) : StyleRoute()
    data class StyleEditor(val styleId: String, val returnTo: StyleRoute) : StyleRoute()
    data object PromptSettings : StyleRoute()
}

private data class PendingAppChange(
    val app: com.imdinkie.voiceslip.data.InstalledAppInfo,
    val targetCategoryId: String?
)

private data class StyleRefreshState(
    val isRefreshing: Boolean,
    val appCount: Int,
    val lastUpdatedAtMillis: Long?,
    val isStale: Boolean,
    val message: String? = null
) {
    companion object {
        fun from(state: com.imdinkie.voiceslip.data.InstalledAppCacheState): StyleRefreshState =
            StyleRefreshState(false, state.appCount, state.lastUpdatedAtMillis, state.isStale)
    }
}

@Composable
private fun StyleHomeScreen(
    repository: VoiceSlipRepository,
    refreshKey: Int,
    refreshState: StyleRefreshState,
    onRoute: (StyleRoute) -> Unit,
    onReload: () -> Unit
) {
    val styles = remember(refreshKey) { repository.listStyles() }
    val categories = remember(refreshKey) { repository.listCategories() }
    val apps = remember(refreshKey) { repository.listInstalledApps() }
    var newCategoryName by remember { mutableStateOf("") }
    var showNewCategory by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionTitle("Style")
            Text("Categories, app assignments, and reusable style prompts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (refreshState.isRefreshing || refreshState.isStale) {
                Text(appRefreshText(refreshState), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Categories", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { showNewCategory = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add category")
                }
            }
        }
        items(categories, key = { it.id }) { category ->
            val styleName = styles.firstOrNull { it.id == category.styleId }?.name ?: "Casual"
            val categoryApps = apps.filter {
                if (category.id == CATEGORY_OTHER) it.categoryId == null else it.categoryId == category.id
            }
            CategoryCard(category, styleName, categoryApps) { onRoute(StyleRoute.CategoryDetail(category.id)) }
        }
        item {
            Spacer(Modifier.height(12.dp))
            Text("Manage", fontWeight = FontWeight.SemiBold)
        }
        item { EntryRow("All Apps", "Search and change app categories") { onRoute(StyleRoute.AllApps) } }
        item { EntryRow("Style Library", "Edit style prompts and create reusable styles") { onRoute(StyleRoute.StyleLibrary()) } }
        item { EntryRow("Prompt Settings", "Edit the shared cleanup policy") { onRoute(StyleRoute.PromptSettings) } }
    }
    if (showNewCategory) {
        AlertDialog(
            onDismissRequest = { showNewCategory = false },
            title = { Text("Add category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Category name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.createCustomCategory(newCategoryName)
                        newCategoryName = ""
                        showNewCategory = false
                        onReload()
                    },
                    enabled = newCategoryName.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newCategoryName = ""
                    showNewCategory = false
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CategoryCard(
    category: VoiceCategory,
    styleName: String,
    apps: List<com.imdinkie.voiceslip.data.InstalledAppInfo>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(category.name, fontWeight = FontWeight.SemiBold)
                Text(styleName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${apps.size} apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                apps.take(3).forEach { AppIcon(it.iconCacheKey, it.label) }
                if (apps.size > 3) Text("+${apps.size - 3}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EntryRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Open", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CategoryDetailScreen(
    repository: VoiceSlipRepository,
    categoryId: String,
    refreshKey: Int,
    refreshState: StyleRefreshState,
    onBack: () -> Unit,
    onChooseStyle: () -> Unit,
    onEditStyle: (String) -> Unit,
    onPickAppCategory: (String) -> Unit,
    onRefreshApps: () -> Unit,
    onReload: () -> Unit
) {
    val categories = remember(refreshKey) { repository.listCategories() }
    val category = categories.firstOrNull { it.id == categoryId } ?: categories.first { it.id == CATEGORY_OTHER }
    val styles = remember(refreshKey) { repository.listStyles() }
    val style = styles.firstOrNull { it.id == category.styleId } ?: repository.getStyle(STYLE_CASUAL)
    var query by remember { mutableStateOf("") }
    val apps = remember(refreshKey, query) { repository.listInstalledApps(query) }
    var pending by remember { mutableStateOf<PendingAppChange?>(null) }
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    BackHandler { onBack() }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader(category.name, onBack) }
            item {
                SettingsCard {
                    Text("Style", fontWeight = FontWeight.SemiBold)
                    Text(style.name, style = MaterialTheme.typography.titleMedium)
                    Text(style.effectivePrompt, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onChooseStyle) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Change")
                        }
                        OutlinedButton(onClick = { onEditStyle(style.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit prompt")
                        }
                    }
                }
            }
            item {
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search apps") }, singleLine = true)
            }
            item {
                RefreshStatusRow(refreshState = refreshState, onRefreshApps = onRefreshApps)
            }
            val sorted = apps.sortedWith(compareByDescending<com.imdinkie.voiceslip.data.InstalledAppInfo> {
                if (category.id == CATEGORY_OTHER) it.categoryId == null else it.categoryId == category.id
            }.thenByDescending { it.lastSeenAtMillis ?: 0L }.thenBy { it.label.lowercase() })
            items(sorted, key = { it.packageName }) { app ->
                val checked = if (category.id == CATEGORY_OTHER) app.categoryId == null else app.categoryId == category.id
                AppAssignmentRow(
                    app = app,
                    checked = checked,
                    onClick = {
                        if (category.id == CATEGORY_OTHER) {
                            if (checked) onPickAppCategory(app.packageName) else pending = PendingAppChange(app, null)
                        } else if (checked) {
                            pending = PendingAppChange(app, null)
                        } else if (app.categoryId != null) {
                            pending = PendingAppChange(app, category.id)
                        } else {
                            repository.assignApp(app.packageName, category.id)
                            onReload()
                        }
                    }
                )
            }
        }
        if (listState.firstVisibleItemIndex > 2) {
            TopArrowButton(
                onClick = {
                    scrollScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )
        }
    }
    pending?.let { change ->
        ConfirmAppChangeDialog(change, categories, onDismiss = { pending = null }) {
            repository.assignApp(change.app.packageName, change.targetCategoryId)
            pending = null
            onReload()
        }
    }
}

@Composable
private fun AllAppsScreen(
    repository: VoiceSlipRepository,
    refreshKey: Int,
    refreshState: StyleRefreshState,
    onBack: () -> Unit,
    onPickAppCategory: (String) -> Unit,
    onRefreshApps: () -> Unit,
    onReload: () -> Unit
) {
    val categories = remember(refreshKey) { repository.listCategories() }
    var query by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val apps = remember(refreshKey, query) { repository.listInstalledApps(query) }
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    BackHandler { onBack() }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader("All Apps", onBack) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRefreshApps, enabled = !refreshState.isRefreshing) { Text("Refresh apps") }
                    Text(appRefreshText(refreshState), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            item {
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search apps") }, singleLine = true)
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("All", selectedCategoryId == null) { selectedCategoryId = null }
                    categories.forEach { category ->
                        CategoryChip(category.name, selectedCategoryId == category.id) { selectedCategoryId = category.id }
                    }
                }
            }
            val filtered = apps.filter {
                selectedCategoryId == null || if (selectedCategoryId == CATEGORY_OTHER) it.categoryId == null else it.categoryId == selectedCategoryId
            }.sortedWith(compareByDescending<com.imdinkie.voiceslip.data.InstalledAppInfo> { it.lastSeenAtMillis ?: 0L }.thenBy { it.label.lowercase() })
            items(filtered, key = { it.packageName }) { app ->
                AppLookupRow(app) { onPickAppCategory(app.packageName) }
            }
        }
        if (listState.firstVisibleItemIndex > 2) {
            TopArrowButton(
                onClick = {
                    scrollScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AppCategoryPickerScreen(
    repository: VoiceSlipRepository,
    packageName: String,
    refreshKey: Int,
    onBack: () -> Unit,
    onReload: () -> Unit
) {
    val categories = remember(refreshKey) { repository.listCategories() }
    val app = remember(refreshKey, packageName) { repository.listInstalledApps().firstOrNull { it.packageName == packageName } }
    var pending by remember { mutableStateOf<PendingAppChange?>(null) }
    BackHandler { onBack() }
    if (app == null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader("Choose Category", onBack)
            Text("App is no longer in the launcher cache.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Choose Category", onBack) }
        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app.iconCacheKey, app.label)
                    Spacer(Modifier.width(10.dp))
                    AppText(app, Modifier.weight(1f))
                    Text(app.categoryName ?: "Unassigned", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(categories, key = { it.id }) { category ->
            val target = if (category.id == CATEGORY_OTHER) null else category.id
            val selected = app.categoryId == target
            val label = if (selected) "${category.name} current" else category.name
            if (selected) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(label) }
            } else {
                OutlinedButton(
                    onClick = {
                        if (app.categoryId != null) {
                            pending = PendingAppChange(app, target)
                        } else {
                            repository.assignApp(app.packageName, target)
                            onReload()
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(category.name) }
            }
        }
    }
    pending?.let { change ->
        ConfirmAppChangeDialog(change, categories, onDismiss = { pending = null }) {
            repository.assignApp(change.app.packageName, change.targetCategoryId)
            pending = null
            onReload()
            onBack()
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) } else OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun RefreshStatusRow(refreshState: StyleRefreshState, onRefreshApps: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            appRefreshText(refreshState),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedButton(onClick = onRefreshApps, enabled = !refreshState.isRefreshing) {
            Text(if (refreshState.isRefreshing) "Refreshing" else "Refresh")
        }
    }
}

@Composable
private fun TopArrowButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .wrapContentWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(22.dp))
            Text("Top", fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun appRefreshText(state: StyleRefreshState): String {
    state.message?.let { return "$it · ${state.appCount} apps" }
    if (state.isRefreshing) return "Refreshing apps · ${state.appCount} cached"
    if (state.lastUpdatedAtMillis == null) return "No cached apps yet"
    val ageHours = ((System.currentTimeMillis() - state.lastUpdatedAtMillis) / (60L * 60L * 1000L)).coerceAtLeast(0L)
    return if (state.isStale) {
        "App cache stale · ${state.appCount} apps · ${ageHours}h old"
    } else {
        "App cache ready · ${state.appCount} apps · ${ageHours}h old"
    }
}

@Composable
private fun AppAssignmentRow(app: com.imdinkie.voiceslip.data.InstalledAppInfo, checked: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onClick() })
            AppIcon(app.iconCacheKey, app.label)
            Spacer(Modifier.width(10.dp))
            AppText(app, Modifier.weight(1f))
            Text(app.categoryName ?: "Unassigned", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppLookupRow(app: com.imdinkie.voiceslip.data.InstalledAppInfo, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.iconCacheKey, app.label)
            Spacer(Modifier.width(10.dp))
            AppText(app, Modifier.weight(1f))
            Text(app.categoryName ?: "Unassigned", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppText(app: com.imdinkie.voiceslip.data.InstalledAppInfo, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AppIcon(iconCacheKey: String, label: String, size: Dp = 36.dp) {
    val file = remember(iconCacheKey) { File(iconCacheKey) }
    if (iconCacheKey.isNotBlank() && file.exists()) {
        AndroidView(
            modifier = Modifier.size(size),
            factory = {
                android.widget.ImageView(it).apply {
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(android.graphics.drawable.Drawable.createFromPath(iconCacheKey))
                }
            },
            update = { it.setImageDrawable(android.graphics.drawable.Drawable.createFromPath(iconCacheKey)) }
        )
    } else {
        Box(
            Modifier.size(size).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(label.trim().take(1).uppercase().ifBlank { "?" }, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun ConfirmAppChangeDialog(
    change: PendingAppChange,
    categories: List<VoiceCategory>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val currentName = change.app.categoryName ?: "Unassigned"
    val targetName = change.targetCategoryId?.let { id -> categories.firstOrNull { it.id == id }?.name } ?: "Unassigned"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (change.targetCategoryId == null) "Unassign app" else "Move app") },
        text = { Text("${change.app.label} is currently in $currentName. ${if (change.targetCategoryId == null) "Unassign it?" else "Move it to $targetName?"}") },
        confirmButton = { Button(onClick = onConfirm) { Text(if (change.targetCategoryId == null) "Unassign" else "Move") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CategoryPickerDialog(
    app: com.imdinkie.voiceslip.data.InstalledAppInfo,
    categories: List<VoiceCategory>,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign ${app.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    OutlinedButton(onClick = { onSelect(if (category.id == CATEGORY_OTHER) null else category.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(category.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun StyleLibraryScreen(
    repository: VoiceSlipRepository,
    chooseForCategoryId: String?,
    refreshKey: Int,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onAdd: () -> Unit,
    onReload: () -> Unit
) {
    val styles = remember(refreshKey) { repository.listStyles() }
    val category = remember(refreshKey, chooseForCategoryId) { chooseForCategoryId?.let { id -> repository.listCategories().firstOrNull { it.id == id } } }
    BackHandler { onBack() }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(if (chooseForCategoryId == null) "Style Library" else "Choose Style", onBack) }
        item { Text(if (category == null) "Manage reusable style prompts." else "For ${category.name}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (chooseForCategoryId == null) {
            item {
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add style") }
            }
        }
        items(styles, key = { it.id }) { style ->
            StyleRow(
                style = style,
                selected = category?.styleId == style.id,
                chooseMode = chooseForCategoryId != null,
                onClick = {
                    if (chooseForCategoryId != null) {
                        repository.updateCategoryStyle(chooseForCategoryId, style.id)
                        onReload()
                        onBack()
                    } else onEdit(style.id)
                }
            )
        }
    }
}

@Composable
private fun StyleRow(style: VoiceStyle, selected: Boolean, chooseMode: Boolean, onClick: () -> Unit) {
    var expanded by remember(style.id) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(style.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    StyleKindLabel(style)
                }
                if (chooseMode && selected) Text("Selected", color = MaterialTheme.colorScheme.primary)
            }
            Text(
                style.effectivePrompt,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(if (expanded) "Show less" else "Show full prompt")
            }
        }
    }
}

@Composable
private fun StyleKindLabel(style: VoiceStyle) {
    when {
        style.isBuiltIn && style.userPromptOverride != null -> StyleLabel("Modified")
        !style.isBuiltIn -> StyleLabel("Custom")
    }
}

@Composable
private fun StyleLabel(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun NewStyleScreen(repository: VoiceSlipRepository, onBack: () -> Unit, onCreated: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var showDiscard by remember { mutableStateOf(false) }
    val dirty = name.isNotBlank() || prompt.isNotBlank()
    fun requestBack() { if (dirty) showDiscard = true else onBack() }
    BackHandler { requestBack() }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Add style", ::requestBack) }
        item {
            SettingsCard {
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                OutlinedTextField(prompt, { prompt = it }, modifier = Modifier.fillMaxWidth().height(320.dp), label = { Text("Prompt editor") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val style = repository.createCustomStyle(name, prompt)
                            onCreated(style.id)
                        },
                        enabled = name.isNotBlank() && prompt.isNotBlank()
                    ) { Text("Save") }
                    OutlinedButton(onClick = ::requestBack) { Text("Cancel") }
                }
            }
        }
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard style?") },
            text = { Text("Your unsaved style will be lost.") },
            confirmButton = { Button(onClick = onBack) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StyleEditorScreen(repository: VoiceSlipRepository, styleId: String, onBack: () -> Unit, onReload: () -> Unit) {
    var style by remember(styleId) { mutableStateOf(repository.getStyle(styleId)) }
    var name by remember(style.id) { mutableStateOf(style.name) }
    var prompt by remember(style.id) { mutableStateOf(style.effectivePrompt) }
    var showDiscard by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val dirty = name != style.name || prompt != style.effectivePrompt
    fun requestBack() { if (dirty) showDiscard = true else onBack() }
    BackHandler { requestBack() }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(style.name, ::requestBack) }
        item {
            SettingsCard {
                if (!style.isBuiltIn) OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                OutlinedTextField(prompt, { prompt = it }, modifier = Modifier.fillMaxWidth().height(320.dp), label = { Text("Prompt editor") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val saved = style.copy(
                            name = if (style.isBuiltIn) style.name else name.trim().ifBlank { style.name },
                            userPromptOverride = if (style.isBuiltIn) prompt else null,
                            defaultPrompt = if (style.isBuiltIn) style.defaultPrompt else prompt
                        )
                        repository.saveStyle(saved)
                        style = repository.getStyle(style.id)
                        name = style.name
                        prompt = style.effectivePrompt
                        onReload()
                    }, enabled = dirty && prompt.isNotBlank()) { Text("Save") }
                    if (style.isBuiltIn) OutlinedButton(onClick = { showReset = true }, enabled = style.userPromptOverride != null || prompt != style.defaultPrompt) { Text("Reset") }
                    if (!style.isBuiltIn) OutlinedButton(onClick = { showDelete = true }) { Text("Delete") }
                }
            }
        }
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved edits will be lost.") },
            confirmButton = { Button(onClick = onBack) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Cancel") } }
        )
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset built-in style?") },
            text = { Text("This clears your override and restores the default prompt.") },
            confirmButton = {
                Button(onClick = {
                    repository.resetBuiltInStyle(style.id)
                    style = repository.getStyle(style.id)
                    prompt = style.defaultPrompt
                    showReset = false
                    onReload()
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } }
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete style?") },
            text = { Text("Categories using this style will fall back to Casual.") },
            confirmButton = {
                Button(onClick = {
                    repository.deleteCustomStyle(style.id)
                    showDelete = false
                    onReload()
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PromptSettingsScreen(repository: VoiceSlipRepository, onBack: () -> Unit) {
    var saved by remember { mutableStateOf(repository.getCleanupPolicy()) }
    var prompt by remember { mutableStateOf(saved) }
    var showDiscard by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    val dirty = prompt != saved
    fun requestBack() { if (dirty) showDiscard = true else onBack() }
    BackHandler { requestBack() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Prompt Settings", ::requestBack)
        SettingsCard {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth().height(360.dp),
                label = { Text("Shared cleanup policy") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    repository.saveCleanupPolicyOverride(prompt)
                    saved = repository.getCleanupPolicy()
                    onBack()
                }, enabled = prompt.isNotBlank() && dirty) { Text("Save") }
                OutlinedButton(onClick = { showReset = true }) { Text("Reset") }
            }
        }
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset cleanup policy?") },
            text = { Text("This restores the shared cleanup policy to the default prompt.") },
            confirmButton = {
                Button(onClick = {
                    repository.saveCleanupPolicyOverride(null)
                    prompt = repository.getDefaultCleanupPolicy()
                    saved = prompt
                    showReset = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } }
        )
    }
    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved prompt edits will be lost.") },
            confirmButton = { Button(onClick = onBack) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.width(4.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DictionaryScreen(repository: VoiceSlipRepository, config: PipelineConfig) {
    var entries by remember { mutableStateOf(repository.listDictionary()) }
    var phrase by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val filtered = entries.filter { it.phrase.contains(query, ignoreCase = true) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Dictionary")
        DictionaryWarning(repository = repository, config = config, entries = entries)
        OutlinedTextField(
            value = phrase,
            onValueChange = { phrase = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Add word or phrase") },
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = {
                    repository.addDictionaryEntry(phrase)
                    phrase = ""
                    entries = repository.listDictionary()
                }) { Text("Add") }
            }
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search") },
            singleLine = true
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { entry ->
                DictionaryRow(entry) {
                    repository.deleteDictionaryEntry(entry.id)
                    entries = repository.listDictionary()
                }
            }
        }
    }
}

@Composable
private fun DictionaryWarning(repository: VoiceSlipRepository, config: PipelineConfig, entries: List<DictionaryEntry>) {
    if (config.mode == PipelineMode.AUDIO_DIRECT) return
    val plan = repository.dictionaryPlanForTranscription(config, entries.map { it.phrase })
    if (!plan.truncated) return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
        Text(
            "Current pipeline uses ${config.transcriptionDisplayName()}. ${dictionaryDuringTranscriptionDetail(plan)} Cleanup still receives all ${plan.totalTerms} Dictionary Entries.",
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun HistoryScreen(
    repository: VoiceSlipRepository,
    scrollToTopRequest: Int,
    onRetry: (HistoryItem) -> Unit,
    onCopy: (String) -> Unit
) {
    var items by remember { mutableStateOf(repository.listHistory()) }
    var pendingClear by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HistoryItem?>(null) }
    var detailItem by remember { mutableStateOf<HistoryItem?>(null) }
    val listState = rememberLazyListState()
    val appIconCache = remember(items) { repository.listInstalledApps().associateBy { it.packageName } }
    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0) {
            listState.scrollToItem(0)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1200)
            items = repository.listHistory()
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("History")
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { pendingClear = true }) { Text("Clear") }
        }
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                HistoryCard(
                    item = item,
                    iconCacheKey = item.targetPackage?.let { appIconCache[it]?.iconCacheKey },
                    onOpen = { detailItem = item },
                    onRetry = {
                        onRetry(item)
                        items = repository.listHistory()
                    },
                    onCopy = { item.displayText()?.let(onCopy) },
                    onDelete = { pendingDelete = item }
                )
            }
        }
    }
    if (pendingClear) {
        AlertDialog(
            onDismissRequest = { pendingClear = false },
            title = { Text("Delete all history?") },
            text = { Text("This deletes every history item and its saved recording file.") },
            confirmButton = {
                Button(onClick = {
                    repository.clearHistory()
                    items = repository.listHistory()
                    pendingClear = false
                }) { Text("Delete history") }
            },
            dismissButton = { TextButton(onClick = { pendingClear = false }) { Text("Cancel") } }
        )
    }
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete item?") },
            text = { Text("This deletes this history item and its saved recording file.") },
            confirmButton = {
                Button(onClick = {
                    repository.deleteHistory(item.id)
                    items = repository.listHistory()
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
    detailItem?.let { item ->
        HistoryDetailDialog(
            repository = repository,
            item = item,
            iconCacheKey = item.targetPackage?.let { appIconCache[it]?.iconCacheKey },
            onDismiss = { detailItem = null },
            onCopyRaw = { item.rawTranscript?.let(onCopy) },
            onCopyFinal = { item.displayText()?.let(onCopy) }
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    action: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = granted, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            OutlinedButton(onClick = onClick) { Text(if (granted) "Open" else action) }
        }
    }
}

@Composable
private fun SetupWarning(missingItems: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Setup incomplete", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                "VoiceSlip needs ${missingItems.joinToString(", ")} before the overlay can record and transcribe.",
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun DictionaryRow(entry: DictionaryEntry, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(entry.phrase, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem,
    iconCacheKey: String?,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.status.name.lowercase().replaceFirstChar { it.titlecase() }, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    DateFormat.format("MMM d, HH:mm", Date(item.createdAtMillis)).toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item.displayText()?.takeIf { it.isNotBlank() }?.let {
                Text(it, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            historyWarningText(item)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            historyContextLine(item)?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!iconCacheKey.isNullOrBlank()) {
                        AppIcon(iconCacheKey, item.targetAppLabel ?: item.targetPackage.orEmpty(), size = 22.dp)
                    }
                    Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                "${item.pipelineSummary ?: item.model} · ${item.durationMillis / 1000}s · retries ${item.retryCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetry, enabled = item.status != RecordingStatus.TRANSCRIBING) { Text("Retry") }
                OutlinedButton(onClick = onCopy, enabled = !item.displayText().isNullOrBlank()) { Text("Copy") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun HistoryDetailDialog(
    repository: VoiceSlipRepository,
    item: HistoryItem,
    iconCacheKey: String?,
    onDismiss: () -> Unit,
    onCopyRaw: () -> Unit,
    onCopyFinal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.pipelineSummary ?: item.model) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "${item.status.name.lowercase().replaceFirstChar { it.titlecase() }} · ${item.durationMillis / 1000}s · retries ${item.retryCount}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item.rawTranscript?.takeIf { it.isNotBlank() }?.let { raw ->
                    item {
                        Text("Raw transcript", fontWeight = FontWeight.SemiBold)
                        Text(raw)
                        OutlinedButton(onClick = onCopyRaw) { Text("Copy raw") }
                    }
                }
                item.displayText()?.takeIf { it.isNotBlank() }?.let { finalText ->
                    item {
                        Text(if (item.errorStage == "output_guard") "Rejected output" else "Final text", fontWeight = FontWeight.SemiBold)
                        Text(finalText)
                        OutlinedButton(onClick = onCopyFinal) { Text("Copy final") }
                    }
                }
                if (item.rawTranscript.isNullOrBlank()) {
                    item {
                        Text(
                            "Raw transcript is not available for this pipeline.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                historyWarningText(item)?.let { error ->
                    item {
                        Text("Error", fontWeight = FontWeight.SemiBold)
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
                item {
                    val original = File(item.audioPath)
                    val derivatives = repository.audioDerivativesFor(item)
                    Text("Audio", fontWeight = FontWeight.SemiBold)
                    DebugLine("Original", audioFileDetail(original))
                    derivatives.forEach { derivative ->
                        DebugLine("Derivative", audioFileDetail(derivative))
                    }
                    if (derivatives.isEmpty()) {
                        Text("No converted derivatives cached.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Text("Debug", fontWeight = FontWeight.SemiBold)
                    if (!iconCacheKey.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppIcon(iconCacheKey, item.targetAppLabel ?: item.targetPackage.orEmpty(), size = 24.dp)
                            Text(item.targetAppLabel ?: item.targetPackage.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DebugLine("Package", item.targetPackage)
                    DebugLine("App", item.targetAppLabel)
                    DebugLine("Category", item.resolvedCategoryName ?: item.resolvedCategoryId)
                    DebugLine("Style", item.resolvedStyleName ?: item.resolvedStyleId)
                    DebugLine("Pipeline", item.pipelineSummary)
                    DebugLine("Provider/model", providerModelLine(item))
                    DebugLine("Detected language", item.detectedLanguage)
                    DebugLine("Insertion stage", item.errorStage)
                    DebugLine("Insertion/error", item.error)
                }
                item.stylePromptSnapshot?.takeIf { it.isNotBlank() }?.let { prompt ->
                    item {
                        Text("Style prompt snapshot", fontWeight = FontWeight.SemiBold)
                        Text(prompt)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun DebugLine(label: String, value: String?) {
    Text("$label: ${value?.takeIf { it.isNotBlank() } ?: ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun audioFileDetail(file: File): String {
    val format = audioFileFormat(file)?.label ?: file.extension.ifBlank { "unknown" }
    val size = if (file.exists()) "${(file.length() / 1024L).coerceAtLeast(1L)} KB" else "missing"
    return "${file.name} · $format · $size"
}

private fun historyContextLine(item: HistoryItem): String? {
    val parts = listOfNotNull(
        item.targetAppLabel?.takeIf { it.isNotBlank() },
        item.resolvedCategoryName?.takeIf { it.isNotBlank() },
        item.resolvedStyleName?.takeIf { it.isNotBlank() }
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun historyWarningText(item: HistoryItem): String? {
    if (item.errorStage == "input_method_unverified" || item.errorStage == "clipboard_fallback") return null
    return item.error?.takeIf { it.isNotBlank() }
}

private fun providerModelLine(item: HistoryItem): String {
    val details = listOfNotNull(
        item.provider.takeIf { it.isNotBlank() }?.let { "$it/${item.model}" },
        item.transcriptionProvider?.takeIf { it.isNotBlank() }?.let { "$it/${item.transcriptionModel.orEmpty()}" },
        item.audioModelProvider?.takeIf { it.isNotBlank() }?.let { "$it/${item.audioModel.orEmpty()}" },
        item.postProcessingProvider?.takeIf { it.isNotBlank() }?.let { "$it/${item.postProcessingModel.orEmpty()}" }
    )
    return details.distinct().joinToString(" · ")
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatusPill(text: String) {
    Box(
        Modifier
            .background(Color(0xFF153F35), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

private data class SetupStatus(
    val accessibility: Boolean,
    val overlay: Boolean,
    val microphone: Boolean,
    val pipelineReady: Boolean,
    val missingProviders: List<ProviderId>,
    val pipelineRunnable: Boolean
) {
    val ready: Boolean = accessibility && overlay && microphone && pipelineReady && pipelineRunnable
    val missingItems: List<String> = buildList {
        if (!accessibility) add("accessibility")
        if (!overlay) add("overlay permission")
        if (!microphone) add("microphone")
        missingProviders.forEach { add("${it.label} API key") }
        if (!pipelineRunnable) add("complete model selection")
    }
}

private fun currentSetupStatus(
    context: Context,
    config: PipelineConfig,
    keys: Map<ProviderId, String>
): SetupStatus {
    val missingProviders = config.requiredProviders().filter { keys[it].isNullOrBlank() }
    return SetupStatus(
        accessibility = isAccessibilityEnabled(context),
        overlay = Settings.canDrawOverlays(context),
        microphone = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        pipelineReady = missingProviders.isEmpty(),
        missingProviders = missingProviders,
        pipelineRunnable = config.isRunnable()
    )
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val settingsEnabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty().split(':').filter { it.isNotBlank() }
    val managerEnabled = context.getSystemService(AccessibilityManager::class.java)
        ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .orEmpty()
        .mapNotNull { info ->
            val serviceInfo = info.resolveInfo?.serviceInfo ?: return@mapNotNull null
            "${serviceInfo.packageName}/${serviceInfo.name}"
        }
    return isVoiceSlipAccessibilityServiceEnabled(
        packageName = context.packageName,
        enabledServiceIds = settingsEnabled + managerEnabled,
        serviceConnected = VoiceSlipAccessibilityService.instance != null
    )
}

private fun HistoryItem.withPipelineResult(
    result: PipelineResult,
    config: PipelineConfig,
    retryCount: Int = this.retryCount
): HistoryItem = copy(
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
    metadataJson = result.metadataJson,
    retryCount = retryCount
)
