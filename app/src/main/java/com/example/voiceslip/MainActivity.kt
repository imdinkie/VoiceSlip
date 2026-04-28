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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.voiceslip.data.BubbleSize
import com.example.voiceslip.data.DictionaryEntry
import com.example.voiceslip.data.HistoryItem
import com.example.voiceslip.data.RecordingStatus
import com.example.voiceslip.data.SecretStore
import com.example.voiceslip.data.VoiceSlipRepository
import com.example.voiceslip.net.MistralTranscriptionClient
import com.example.voiceslip.ui.theme.VoiceSlipTheme
import java.io.File
import java.util.Date

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
        val apiKey = secretStore.getMistralApiKey()
        if (apiKey.isNullOrBlank()) return
        repository.upsertHistory(item.copy(status = RecordingStatus.TRANSCRIBING, error = null))
        Thread {
            val updated = runCatching {
                val result = MistralTranscriptionClient().transcribe(
                    apiKey = apiKey,
                    audioFile = File(item.audioPath),
                    contextBias = repository.listDictionary().map { it.phrase }
                )
                item.copy(
                    status = RecordingStatus.SUCCEEDED,
                    transcript = result.text,
                    error = null,
                    model = result.model,
                    retryCount = item.retryCount + 1
                )
            }.getOrElse {
                item.copy(
                    status = RecordingStatus.FAILED,
                    error = it.message ?: it::class.java.simpleName,
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
    var apiKey by remember { mutableStateOf(secretStore.getMistralApiKey().orEmpty()) }
    var haptics by remember { mutableStateOf(repository.getHapticsEnabled()) }
    var bubbleSize by remember { mutableStateOf(repository.getBubbleSize()) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTick++
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTick++
    }

    LaunchedEffect(refreshTick) {
        apiKey = secretStore.getMistralApiKey().orEmpty()
        haptics = repository.getHapticsEnabled()
        bubbleSize = repository.getBubbleSize()
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
    val setupStatus = remember(refreshTick, apiKey) { currentSetupStatus(context, apiKey) }

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
                    StatusPill(if (setupStatus.ready) "Ready" else "Setup")
                }
                TabRow(selectedTabIndex = selectedTab) {
                    listOf("Setup", "Dictionary", "History").forEachIndexed { index, title ->
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
                    apiKey = apiKey,
                    haptics = haptics,
                    bubbleSize = bubbleSize,
                    setupStatus = setupStatus,
                    onApiKeyChange = {
                        apiKey = it
                        secretStore.saveMistralApiKey(it)
                    },
                    onHapticsChange = {
                        haptics = it
                        repository.setHapticsEnabled(it)
                    },
                    onBubbleSizeChange = {
                        bubbleSize = it
                        repository.setBubbleSize(it)
                    },
                    onOpenAccessibility = onOpenAccessibility,
                    onOpenOverlay = onOpenOverlay,
                    onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
                1 -> DictionaryScreen(repository = repository)
                2 -> HistoryScreen(repository = repository, onRetry = onRetry, onCopy = onCopy)
            }
        }
    }
}

@Composable
private fun SetupScreen(
    apiKey: String,
    haptics: Boolean,
    bubbleSize: BubbleSize,
    setupStatus: SetupStatus,
    onApiKeyChange: (String) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onBubbleSizeChange: (BubbleSize) -> Unit,
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
            SectionTitle("Mistral")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mistral API key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Text(
                        "V1 uses voxtral-mini-latest through Mistral audio transcriptions. The key is encrypted with Android Keystore before local storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SectionTitle("Interaction")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Short haptics", fontWeight = FontWeight.SemiBold)
                            Text("Off by default. Vibrates briefly on record, cancel, and submit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = haptics, onCheckedChange = onHapticsChange)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bubble size", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BubbleSize.entries.forEach { size ->
                                if (size == bubbleSize) {
                                    Button(onClick = { onBubbleSizeChange(size) }) { Text(size.label) }
                                } else {
                                    OutlinedButton(onClick = { onBubbleSizeChange(size) }) { Text(size.label) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryScreen(repository: VoiceSlipRepository) {
    var entries by remember { mutableStateOf(repository.listDictionary()) }
    var phrase by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val filtered = entries.filter { it.phrase.contains(query, ignoreCase = true) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Dictionary")
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
private fun HistoryScreen(
    repository: VoiceSlipRepository,
    onRetry: (HistoryItem) -> Unit,
    onCopy: (String) -> Unit
) {
    var items by remember { mutableStateOf(repository.listHistory()) }
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
            OutlinedButton(onClick = {
                repository.clearHistory()
                items = repository.listHistory()
            }) { Text("Clear") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                HistoryCard(
                    item = item,
                    onRetry = {
                        onRetry(item)
                        items = repository.listHistory()
                    },
                    onCopy = { item.transcript?.let(onCopy) },
                    onDelete = {
                        repository.deleteHistory(item.id)
                        items = repository.listHistory()
                    }
                )
            }
        }
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
    onRetry: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.status.name.lowercase().replaceFirstChar { it.titlecase() }, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    DateFormat.format("MMM d, HH:mm", Date(item.createdAtMillis)).toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item.transcript?.takeIf { it.isNotBlank() }?.let {
                Text(it, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            item.error?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = MaterialTheme.colorScheme.error, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Text(
                "${item.model} · ${item.durationMillis / 1000}s · retries ${item.retryCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetry, enabled = item.status != RecordingStatus.TRANSCRIBING) { Text("Retry") }
                OutlinedButton(onClick = onCopy, enabled = !item.transcript.isNullOrBlank()) { Text("Copy") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
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
    val apiKey: Boolean
) {
    val ready: Boolean = accessibility && overlay && microphone && apiKey
    val missingItems: List<String> = buildList {
        if (!accessibility) add("accessibility")
        if (!overlay) add("overlay permission")
        if (!microphone) add("microphone")
        if (!apiKey) add("Mistral API key")
    }
}

private fun currentSetupStatus(context: Context, apiKey: String): SetupStatus {
    return SetupStatus(
        accessibility = isAccessibilityEnabled(context),
        overlay = Settings.canDrawOverlays(context),
        microphone = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        apiKey = apiKey.isNotBlank()
    )
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty()
    return enabled.split(':').any { it.equals("${context.packageName}/com.example.voiceslip.service.VoiceSlipAccessibilityService", ignoreCase = true) }
}
