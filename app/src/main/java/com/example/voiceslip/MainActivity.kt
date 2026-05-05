package com.example.voiceslip

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.voiceslip.data.BUBBLE_OPACITY_MAX_PERCENT
import com.example.voiceslip.data.BUBBLE_OPACITY_MIN_PERCENT
import com.example.voiceslip.data.BUBBLE_SIZE_MAX_DP
import com.example.voiceslip.data.BUBBLE_SIZE_MIN_DP
import com.example.voiceslip.data.DictionaryEntry
import com.example.voiceslip.data.HistoryItem
import com.example.voiceslip.data.ModelOption
import com.example.voiceslip.data.PipelineConfig
import com.example.voiceslip.data.PipelineMode
import com.example.voiceslip.data.PostProcessingProvider
import com.example.voiceslip.data.ProviderId
import com.example.voiceslip.data.RecordingStatus
import com.example.voiceslip.data.SecretStore
import com.example.voiceslip.data.TranscriptionEngineId
import com.example.voiceslip.data.AudioDirectEngineId
import com.example.voiceslip.data.CATEGORY_OTHER
import com.example.voiceslip.data.EngineKind
import com.example.voiceslip.data.EngineDictionaryRoutingEntity
import com.example.voiceslip.data.OPENROUTER_AUDIO_TRANSCRIPTION_ROUTING_ID
import com.example.voiceslip.data.STYLE_CASUAL
import com.example.voiceslip.data.VoiceCategory
import com.example.voiceslip.data.VoiceSlipRepository
import com.example.voiceslip.data.VoiceStyle
import com.example.voiceslip.net.GroqClient
import com.example.voiceslip.net.OpenRouterClient
import com.example.voiceslip.net.PipelineException
import com.example.voiceslip.net.PipelineExecutor
import com.example.voiceslip.net.PipelineResult
import com.example.voiceslip.net.buildAudioDirectPrompt
import com.example.voiceslip.net.buildAudioTranscriptionPromptPreview
import com.example.voiceslip.net.buildLanguageHintExamples
import com.example.voiceslip.net.buildPostProcessingLanguageBlock
import com.example.voiceslip.net.outputGuardRejection
import com.example.voiceslip.service.VoiceSlipAccessibilityService
import com.example.voiceslip.ui.theme.VoiceSlipTheme
import java.io.File
import java.util.Date
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
                val dictionary = repository.listDictionary().map { it.phrase }
                val transcriptionDictionary = if (config.mode == PipelineMode.AUDIO_DIRECT) {
                    dictionary
                } else {
                    val plan = repository.dictionaryPlanForTranscription(config, dictionary)
                    dictionary.take(plan.includedTerms)
                }
                val result = PipelineExecutor { secretStore.getApiKey(it) }.execute(
                    config = config,
                    audioFile = File(item.audioPath),
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
                    metadataJson = result.metadataJson,
                    retryCount = item.retryCount + 1
                )
            }.getOrElse {
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
    var selectedTab by remember { mutableIntStateOf(0) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var mistralKey by remember { mutableStateOf(secretStore.getApiKey(ProviderId.MISTRAL).orEmpty()) }
    var groqKey by remember { mutableStateOf(secretStore.getApiKey(ProviderId.GROQ).orEmpty()) }
    var openRouterKey by remember { mutableStateOf(secretStore.getApiKey(ProviderId.OPENROUTER).orEmpty()) }
    var pipelineConfig by remember { mutableStateOf(repository.getPipelineConfig()) }
    var languageHints by remember { mutableStateOf(repository.getLanguageHints()) }
    var preserveSpokenLanguage by remember { mutableStateOf(repository.getPreserveSpokenLanguage()) }
    var groqModels by remember { mutableStateOf(repository.getCachedModels(ProviderId.GROQ)) }
    var openRouterModels by remember { mutableStateOf(repository.getCachedModels(ProviderId.OPENROUTER)) }
    var openRouterAudioModels by remember { mutableStateOf(repository.getCachedOpenRouterAudioModels()) }
    var modelStatus by remember { mutableStateOf<String?>(null) }
    var appEnabled by remember { mutableStateOf(repository.getAppEnabled()) }
    var haptics by remember { mutableStateOf(repository.getHapticsEnabled()) }
    var bubbleSizeDp by remember { mutableIntStateOf(repository.getBubbleSizeDp()) }
    var bubbleOpacityPercent by remember { mutableIntStateOf(repository.getBubbleOpacityPercent()) }
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
        pipelineConfig = repository.getPipelineConfig()
        languageHints = repository.getLanguageHints()
        preserveSpokenLanguage = repository.getPreserveSpokenLanguage()
        groqModels = repository.getCachedModels(ProviderId.GROQ)
        openRouterModels = repository.getCachedModels(ProviderId.OPENROUTER)
        openRouterAudioModels = repository.getCachedOpenRouterAudioModels()
        appEnabled = repository.getAppEnabled()
        haptics = repository.getHapticsEnabled()
        bubbleSizeDp = repository.getBubbleSizeDp()
        bubbleOpacityPercent = repository.getBubbleOpacityPercent()
    }
    DisposableEffect(context) {
        val lifecycleOwner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }
    val setupStatus = remember(refreshTick, mistralKey, groqKey, openRouterKey, pipelineConfig) {
        currentSetupStatus(
            context = context,
            config = pipelineConfig,
            keys = mapOf(
                ProviderId.MISTRAL to mistralKey,
                ProviderId.GROQ to groqKey,
                ProviderId.OPENROUTER to openRouterKey
            )
        )
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
                    listOf("Setup", "Models", "Style", "Dictionary", "History").forEachIndexed { index, title ->
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
                0 -> SetupScreen(
                    mistralKey = mistralKey,
                    groqKey = groqKey,
                    openRouterKey = openRouterKey,
                    appEnabled = appEnabled,
                    haptics = haptics,
                    bubbleSizeDp = bubbleSizeDp,
                    bubbleOpacityPercent = bubbleOpacityPercent,
                    setupStatus = setupStatus,
                    onProviderKeyChange = { provider, key ->
                        when (provider) {
                            ProviderId.MISTRAL -> mistralKey = key
                            ProviderId.GROQ -> groqKey = key
                            ProviderId.OPENROUTER -> openRouterKey = key
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
                    }
                )
                1 -> ModelsScreen(
                    config = pipelineConfig,
                    repository = repository,
                    languageHints = languageHints,
                    preserveSpokenLanguage = preserveSpokenLanguage,
                    groqModels = groqModels,
                    openRouterModels = openRouterModels,
                    openRouterAudioModels = openRouterAudioModels,
                    modelStatus = modelStatus,
                    hasGroqKey = groqKey.isNotBlank(),
                    hasOpenRouterKey = openRouterKey.isNotBlank(),
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
                            val message = result.fold(
                                onSuccess = { "OpenRouter audio models refreshed (${it.size})" },
                                onFailure = { "OpenRouter audio refresh failed: ${it.message}" }
                            )
                            (context as? ComponentActivity)?.runOnUiThread {
                                result.getOrNull()?.let { openRouterAudioModels = it }
                                modelStatus = message
                            }
                        }.start()
                    }
                )
                2 -> StyleScreen(repository = repository)
                3 -> DictionaryScreen(repository = repository, config = pipelineConfig)
                4 -> HistoryScreen(repository = repository, onRetry = onRetry, onCopy = onCopy)
            }
        }
    }
}

@Composable
private fun SetupScreen(
    mistralKey: String,
    groqKey: String,
    openRouterKey: String,
    appEnabled: Boolean,
    haptics: Boolean,
    bubbleSizeDp: Int,
    bubbleOpacityPercent: Int,
    setupStatus: SetupStatus,
    onProviderKeyChange: (ProviderId, String) -> Unit,
    onAppEnabledChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBubbleSizeChange: (Int) -> Unit,
    onBubbleOpacityChange: (Int) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestNotifications: () -> Unit
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
    modelStatus: String?,
    hasGroqKey: Boolean,
    hasOpenRouterKey: Boolean,
    onConfigChange: (PipelineConfig) -> Unit,
    onLanguageHintsChange: (String) -> Unit,
    onPreserveSpokenLanguageChange: (Boolean) -> Unit,
    onRefreshGroq: () -> Unit,
    onRefreshOpenRouter: () -> Unit,
    onRefreshOpenRouterAudio: () -> Unit
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
            openRouterAudioModels = openRouterAudioModels,
            openRouterAudioFavoriteIds = audioFavoriteIds,
            modelStatus = modelStatus,
            hasGroqKey = hasGroqKey,
            hasOpenRouterKey = hasOpenRouterKey,
            listState = mainListState,
            onConfigChange = onConfigChange,
            onLanguageHintsChange = onLanguageHintsChange,
            onPreserveSpokenLanguageChange = onPreserveSpokenLanguageChange,
            onPreview = { showPreview = true },
            onManageOpenRouterAudio = { route = ModelsRoute.OpenRouterAudio(it) },
            onManagePostProcessing = { route = ModelsRoute.PostProcessing }
        )
        is ModelsRoute.OpenRouterAudio -> OpenRouterAudioPickerScreen(
            slot = currentRoute.slot,
            config = config,
            models = openRouterAudioModels,
            favoriteIds = audioFavoriteIds,
            hasOpenRouterKey = hasOpenRouterKey,
            modelStatus = modelStatus,
            onBack = { route = ModelsRoute.Main },
            onRefresh = onRefreshOpenRouterAudio,
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
            modelStatus = modelStatus,
            onBack = { route = ModelsRoute.Main },
            onConfigChange = onConfigChange,
            onRefreshGroq = onRefreshGroq,
            onRefreshOpenRouter = onRefreshOpenRouter,
            onToggleFavorite = { provider, modelId ->
                repository.togglePostProcessingFavorite(provider, modelId)
                refreshFavorites()
            },
            onSelected = { route = ModelsRoute.Main }
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
    openRouterAudioModels: List<ModelOption>,
    openRouterAudioFavoriteIds: List<String>,
    modelStatus: String?,
    hasGroqKey: Boolean,
    hasOpenRouterKey: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onConfigChange: (PipelineConfig) -> Unit,
    onLanguageHintsChange: (String) -> Unit,
    onPreserveSpokenLanguageChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onManageOpenRouterAudio: (OpenRouterAudioSlot) -> Unit,
    onManagePostProcessing: () -> Unit
) {
    val favoriteAudioRows = remember(openRouterAudioModels, openRouterAudioFavoriteIds) {
        modelRows(openRouterAudioModels, openRouterAudioFavoriteIds, selectedId = null, query = "", favoritesOnly = true, fallbackProvider = "OpenRouter")
    }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Models")
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pipeline preview", fontWeight = FontWeight.SemiBold)
                            Text("Shows providers, prompts, dictionary routing, and insertion fallback.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = onPreview) { Text("Preview") }
                    }
                }
            }
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Preserve spoken language", fontWeight = FontWeight.SemiBold)
                            Text("Keeps output in the language you dictated.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SettingsSwitch(
                            checked = preserveSpokenLanguage,
                            onCheckedChange = onPreserveSpokenLanguageChange
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Language hints", fontWeight = FontWeight.SemiBold)
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
                            .ifBlank { "Prompts preserve the spoken language and do not translate." }
                        Text(helperText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
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
                    SettingsCard {
                        Text("Transcription engine", fontWeight = FontWeight.SemiBold)
                        TranscriptionEngineChoiceColumn(
                            config = config,
                            openRouterAudioRows = favoriteAudioRows,
                            onSelectBuiltIn = { onConfigChange(config.copy(transcriptionEngineKind = EngineKind.BUILT_IN, transcriptionEngine = it)) },
                            onSelectOpenRouter = { onConfigChange(config.copy(transcriptionEngineKind = EngineKind.OPENROUTER_AUDIO, openRouterAudioTranscriptionModel = it)) },
                            onManageOpenRouterAudio = { onManageOpenRouterAudio(OpenRouterAudioSlot.TRANSCRIPTION) }
                        )
                    }
                }
            } else {
                item {
                    SettingsCard {
                        Text("Audio direct model", fontWeight = FontWeight.SemiBold)
                        AudioDirectChoiceColumn(
                            config = config,
                            openRouterAudioRows = favoriteAudioRows,
                            onSelectBuiltIn = { onConfigChange(config.copy(audioDirectEngineKind = EngineKind.BUILT_IN, audioDirectEngine = it)) },
                            onSelectOpenRouter = { onConfigChange(config.copy(audioDirectEngineKind = EngineKind.OPENROUTER_AUDIO, openRouterAudioDirectModel = it)) },
                            onManageOpenRouterAudio = { onManageOpenRouterAudio(OpenRouterAudioSlot.AUDIO_DIRECT) }
                        )
                    }
                }
            }
            if (config.mode == PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING) {
                item {
                    SettingsCard {
                        Text("Post-Processing Model", fontWeight = FontWeight.SemiBold)
                        Text(postProcessingModelSummary(config), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                DictionaryRoutingCard(repository = repository, config = config)
            }
            item {
                Text(
                    activePipelineText(config),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private sealed class ModelsRoute {
    object Main : ModelsRoute()
    data class OpenRouterAudio(val slot: OpenRouterAudioSlot) : ModelsRoute()
    object PostProcessing : ModelsRoute()
}

private enum class OpenRouterAudioSlot(val title: String) {
    TRANSCRIPTION("Choose transcription model"),
    AUDIO_DIRECT("Choose audio direct model")
}

@Composable
private fun OpenRouterAudioPickerScreen(
    slot: OpenRouterAudioSlot,
    config: PipelineConfig,
    models: List<ModelOption>,
    favoriteIds: List<String>,
    hasOpenRouterKey: Boolean,
    modelStatus: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onConfigChange: (PipelineConfig) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    BackHandler { onBack() }
    var query by remember { mutableStateOf("") }
    val selectedModel = when (slot) {
        OpenRouterAudioSlot.TRANSCRIPTION -> config.openRouterAudioTranscriptionModel
        OpenRouterAudioSlot.AUDIO_DIRECT -> config.openRouterAudioDirectModel
    }
    val rows = remember(models, favoriteIds, selectedModel, query) {
        modelRows(models, favoriteIds, selectedModel, query, fallbackProvider = "OpenRouter")
    }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(slot.title, onBack) }
        item {
            SettingsCard {
                Text("Current selected", fontWeight = FontWeight.SemiBold)
                Text(selectedModel.ifBlank { "No OpenRouter audio model selected" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (selectedModel.isNotBlank() && rows.firstOrNull { it.id == selectedModel }?.isAvailable == false) {
                    Text("Unavailable in latest refresh", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search models") },
                singleLine = true
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onRefresh, enabled = hasOpenRouterKey) { Text("Refresh") }
                modelStatus?.let { Text(it, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (!hasOpenRouterKey) {
                Text("Add an OpenRouter API key to refresh compatible audio models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (models.isEmpty()) {
                Text("Refresh to load compatible OpenRouter audio models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (rows.isEmpty()) {
            item { Text("No models match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(rows, key = { it.id }) { row ->
            ModelPickerRow(
                row = row,
                selected = row.id == selectedModel,
                favorite = row.id in favoriteIds,
                onClick = {
                    val updated = when (slot) {
                        OpenRouterAudioSlot.TRANSCRIPTION -> config.copy(
                            transcriptionEngineKind = EngineKind.OPENROUTER_AUDIO,
                            openRouterAudioTranscriptionModel = row.id
                        )
                        OpenRouterAudioSlot.AUDIO_DIRECT -> config.copy(
                            audioDirectEngineKind = EngineKind.OPENROUTER_AUDIO,
                            openRouterAudioDirectModel = row.id
                        )
                    }
                    onConfigChange(updated)
                },
                onToggleFavorite = { onToggleFavorite(row.id) }
            )
        }
    }
}

@Composable
private fun PostProcessingPickerScreen(
    config: PipelineConfig,
    groqModels: List<ModelOption>,
    openRouterModels: List<ModelOption>,
    groqFavoriteIds: List<String>,
    openRouterFavoriteIds: List<String>,
    hasGroqKey: Boolean,
    hasOpenRouterKey: Boolean,
    modelStatus: String?,
    onBack: () -> Unit,
    onConfigChange: (PipelineConfig) -> Unit,
    onRefreshGroq: () -> Unit,
    onRefreshOpenRouter: () -> Unit,
    onToggleFavorite: (PostProcessingProvider, String) -> Unit,
    onSelected: () -> Unit
) {
    BackHandler { onBack() }
    var pickerState by remember { mutableStateOf(initialPostProcessingPickerState(config)) }
    val activeProvider = pickerState.activeProvider
    val models = if (activeProvider == PostProcessingProvider.GROQ) groqModels else openRouterModels
    val favoriteIds = if (activeProvider == PostProcessingProvider.GROQ) groqFavoriteIds else openRouterFavoriteIds
    val selectedModel = selectedPostProcessingModel(config, activeProvider)
    val hasKey = if (activeProvider == PostProcessingProvider.GROQ) hasGroqKey else hasOpenRouterKey
    val rows = remember(models, favoriteIds, selectedModel, pickerState.query, activeProvider) {
        modelRows(models, favoriteIds, selectedModel, pickerState.query, fallbackProvider = activeProvider.label)
    }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }
        }
        item {
            OutlinedTextField(
                value = pickerState.query,
                onValueChange = { pickerState = pickerState.copy(query = it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search models") },
                singleLine = true
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = if (activeProvider == PostProcessingProvider.GROQ) onRefreshGroq else onRefreshOpenRouter,
                    enabled = hasKey
                ) { Text("Refresh ${activeProvider.label}") }
                modelStatus?.let { Text(it, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (!hasKey) {
                Text("Add a ${activeProvider.label} API key to refresh models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (models.isEmpty()) {
                Text("Refresh to load ${activeProvider.label} models.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (rows.isEmpty()) {
            item { Text("No models match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(rows, key = { it.id }) { row ->
            ModelPickerRow(
                row = row,
                selected = isActivePostProcessingModel(config, activeProvider, row.id),
                savedForProvider = isSavedPostProcessingModel(config, activeProvider, row.id),
                favorite = row.id in favoriteIds,
                onClick = {
                    onConfigChange(pickerState.selectModel(config, row.id))
                    onSelected()
                },
                onToggleFavorite = { onToggleFavorite(activeProvider, row.id) }
            )
        }
    }
}

@Composable
private fun ModelPickerRow(
    row: ModelDisplayRow,
    selected: Boolean,
    savedForProvider: Boolean = false,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
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

private fun modelRowSubtitle(row: ModelDisplayRow): String {
    val availability = if (row.isAvailable) "" else " · Unavailable in latest refresh"
    return "${row.provider} · ${row.id}$availability"
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun TranscriptionEngineChoiceColumn(
    config: PipelineConfig,
    openRouterAudioRows: List<ModelDisplayRow>,
    onSelectBuiltIn: (TranscriptionEngineId) -> Unit,
    onSelectOpenRouter: (String) -> Unit,
    onManageOpenRouterAudio: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TranscriptionEngineId.entries.forEach { engine ->
            EngineChoiceButton(
                title = engine.displayName,
                detail = "${engine.provider.label} · ${engine.model} · ${engineRole(engine)}",
                selected = config.transcriptionEngineKind == EngineKind.BUILT_IN && engine == config.transcriptionEngine,
                onClick = { onSelectBuiltIn(engine) }
            )
        }
        Text("OpenRouter audio", fontWeight = FontWeight.SemiBold)
        if (openRouterAudioRows.isEmpty()) {
            Text("No favorites yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            openRouterAudioRows.forEach { row ->
                EngineChoiceButton(
                    title = row.name,
                    detail = "OpenRouter · ${row.id} · audio${if (row.isAvailable) "" else " · Unavailable in latest refresh"}",
                    selected = config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO && config.openRouterAudioTranscriptionModel == row.id,
                    onClick = { onSelectOpenRouter(row.id) }
                )
            }
        }
        OutlinedButton(onClick = onManageOpenRouterAudio, modifier = Modifier.fillMaxWidth()) { Text("Manage OpenRouter audio models") }
    }
}

@Composable
private fun AudioDirectChoiceColumn(
    config: PipelineConfig,
    openRouterAudioRows: List<ModelDisplayRow>,
    onSelectBuiltIn: (AudioDirectEngineId) -> Unit,
    onSelectOpenRouter: (String) -> Unit,
    onManageOpenRouterAudio: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AudioDirectEngineId.entries.forEach { engine ->
            val warning = if (engine == AudioDirectEngineId.MISTRAL_VOXTRAL_MINI_AUDIO) {
                " · Not recommended: prone to formatting and repetition errors."
            } else {
                ""
            }
            EngineChoiceButton(
                title = engine.displayName,
                detail = "${engine.provider.label} · ${engine.model} · audio chat -> final text$warning",
                selected = config.audioDirectEngineKind == EngineKind.BUILT_IN && engine == config.audioDirectEngine,
                onClick = { onSelectBuiltIn(engine) }
            )
        }
        Text("OpenRouter audio", fontWeight = FontWeight.SemiBold)
        if (openRouterAudioRows.isEmpty()) {
            Text("No favorites yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            openRouterAudioRows.forEach { row ->
                EngineChoiceButton(
                    title = row.name,
                    detail = "OpenRouter · ${row.id} · audio${if (row.isAvailable) "" else " · Unavailable in latest refresh"}",
                    selected = config.audioDirectEngineKind == EngineKind.OPENROUTER_AUDIO && config.openRouterAudioDirectModel == row.id,
                    onClick = { onSelectOpenRouter(row.id) }
                )
            }
        }
        OutlinedButton(onClick = onManageOpenRouterAudio, modifier = Modifier.fillMaxWidth()) { Text("Manage OpenRouter audio models") }
    }
}

@Composable
private fun EngineChoiceButton(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val content: @Composable RowScope.() -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (selected) Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), content = content)
    else OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), content = content)
}

private fun engineRole(engine: TranscriptionEngineId): String {
    return when {
        engine.audioChat -> "audio chat -> raw transcript"
        engine.provider == ProviderId.GROQ -> "speech-to-text endpoint"
        else -> "transcription endpoint"
    }
}

private fun activePipelineText(config: PipelineConfig): String {
    return when (config.mode) {
        PipelineMode.PURE_TRANSCRIPTION ->
            "Active: ${config.transcriptionDisplayName()} (${config.transcriptionModel()}). Final text is the raw transcript."
        PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING ->
            "Active: ${config.transcriptionDisplayName()} (${config.transcriptionModel()}) -> ${config.postProcessingProvider.label} ${config.postProcessingModel.ifBlank { "(select model)" }}. Style is resolved from the Style tab at recording start."
        PipelineMode.AUDIO_DIRECT ->
            "Active: ${config.audioDirectDisplayName()} (${config.audioDirectModel()}) with the recording-start style prompt in one call."
    }
}

private fun postProcessingModelSummary(config: PipelineConfig): String {
    if (config.postProcessingProvider == PostProcessingProvider.NONE || config.postProcessingModel.isBlank()) {
        return "No Post-Processing Model selected"
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
    buildString {
        appendLine("Clean this raw transcript and return the final insertable text.")
        appendLine()
        buildPostProcessingLanguageBlock(DETECTED_LANGUAGE_PLACEHOLDER, preserveSpokenLanguage)?.let {
            appendLine(it)
            appendLine()
        }
        appendLine("Follow these global cleanup rules:")
        appendLine(cleanupPolicy)
        if (dictionarySize > 0) {
            appendLine()
            appendLine("Dictionary spelling constraints: {{dictionary_terms}}.")
        }
        appendLine()
        append("Return only JSON with key final_text.")
    }

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
private fun DictionaryRoutingCard(repository: VoiceSlipRepository, config: PipelineConfig) {
    val entries = repository.listDictionary()
    val routingId = if (config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO) OPENROUTER_AUDIO_TRANSCRIPTION_ROUTING_ID else config.transcriptionEngine.name
    val engineName = if (config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO) "OpenRouter audio" else config.transcriptionEngine.displayName
    var routing by remember(routingId) { mutableStateOf(repository.routingForEngine(routingId)) }
    val plan = remember(routing, entries, config) {
        if (config.mode == PipelineMode.AUDIO_DIRECT) null else repository.dictionaryPlanForTranscription(config, entries.map { it.phrase })
    }
    SettingsCard {
        Text("Dictionary routing", fontWeight = FontWeight.SemiBold)
        if (config.mode == PipelineMode.AUDIO_DIRECT) {
            Text("Audio direct: full dictionary is sent as spelling constraints in the audio prompt.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val routingEnabled = routing.sendDictionaryToTranscription
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(engineName)
                    if (routingEnabled) {
                        plan?.mechanism
                            ?.takeUnless { it.isBlank() || it == "Off" }
                            ?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                Text("No dictionary terms saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (routingEnabled && plan != null && plan.includedTerms > 0) {
                val termsText = "Terms included: ${plan.includedTerms} of ${plan.totalTerms}."
                val detail = plan.limit?.let { "Prompt limit: $it. $termsText" } ?: termsText
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Full dictionary is always used during cleanup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Audio: WAV, 16 kHz mono")
                        if (config.transcriptionEngineKind == EngineKind.OPENROUTER_AUDIO) {
                            Text("Audio input: base64 input_audio")
                        }
                        transcriptionPlan?.let {
                            Text("Dictionary: ${it.mechanism}")
                            it.limit?.let { limit -> Text("Prompt limit: $limit chars") }
                            Text("Terms included: ${it.includedTerms} of ${it.totalTerms}")
                            if (transcriptionUsesAudioPrompt) {
                                Text("Prompt sent:\n${audioChatTranscriptionPromptPreview(languageHints, preserveSpokenLanguage, it.prompt)}")
                            } else {
                                it.prompt?.let { prompt -> Text("Prompt sent:\n$prompt") }
                            }
                        }
                    }
                }
                if (config.mode == PipelineMode.TRANSCRIPTION_PLUS_POST_PROCESSING) {
                    item {
                        Text("Step 2: Post-processing", fontWeight = FontWeight.SemiBold)
                        Text("Provider: ${config.postProcessingProvider.label}")
                        Text("Model: ${config.postProcessingModel.ifBlank { "(select model)" }}")
                        Text("Dictionary: ${dictionary.size} of ${dictionary.size} terms included as spelling constraints")
                        Text("Resolved style: {{style_prompt}}")
                        Text("System prompt:\n${postProcessingSystemPromptPreview(cleanupPolicy, preserveSpokenLanguage, dictionary.size)}")
                        Text("User prompt:\nApply this formatting style:\n${resolution.stylePrompt}\n\nRaw transcript:\n{{raw_transcript}}")
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
                        Text("Dictionary: full dictionary included in prompt")
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
    val app: com.example.voiceslip.data.InstalledAppInfo,
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
        fun from(state: com.example.voiceslip.data.InstalledAppCacheState): StyleRefreshState =
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
    apps: List<com.example.voiceslip.data.InstalledAppInfo>,
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
            val sorted = apps.sortedWith(compareByDescending<com.example.voiceslip.data.InstalledAppInfo> {
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
            }.sortedWith(compareByDescending<com.example.voiceslip.data.InstalledAppInfo> { it.lastSeenAtMillis ?: 0L }.thenBy { it.label.lowercase() })
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
private fun AppAssignmentRow(app: com.example.voiceslip.data.InstalledAppInfo, checked: Boolean, onClick: () -> Unit) {
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
private fun AppLookupRow(app: com.example.voiceslip.data.InstalledAppInfo, onClick: () -> Unit) {
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
private fun AppText(app: com.example.voiceslip.data.InstalledAppInfo, modifier: Modifier = Modifier) {
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
    app: com.example.voiceslip.data.InstalledAppInfo,
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
        TextButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            "Current pipeline uses ${config.transcriptionDisplayName()}. Only the first ${plan.includedTerms} of ${plan.totalTerms} terms fit in the transcription prompt. The full dictionary is still used during cleanup.",
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun HistoryScreen(
    repository: VoiceSlipRepository,
    onRetry: (HistoryItem) -> Unit,
    onCopy: (String) -> Unit
) {
    var items by remember { mutableStateOf(repository.listHistory()) }
    var pendingClear by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HistoryItem?>(null) }
    var detailItem by remember { mutableStateOf<HistoryItem?>(null) }
    val appIconCache = remember(items) { repository.listInstalledApps().associateBy { it.packageName } }
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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            item.error?.takeIf { it.isNotBlank() }?.let {
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
                item.error?.takeIf { it.isNotBlank() }?.let { error ->
                    item {
                        Text("Error", fontWeight = FontWeight.SemiBold)
                        Text(error, color = MaterialTheme.colorScheme.error)
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

private fun historyContextLine(item: HistoryItem): String? {
    val parts = listOfNotNull(
        item.targetAppLabel?.takeIf { it.isNotBlank() },
        item.resolvedCategoryName?.takeIf { it.isNotBlank() },
        item.resolvedStyleName?.takeIf { it.isNotBlank() }
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
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
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabled.split(':').any { it.equals("${context.packageName}/com.example.voiceslip.service.VoiceSlipAccessibilityService", ignoreCase = true) }
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
