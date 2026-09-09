package com.example.blueprintai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.blueprintai.data.DiagnosticLog
import com.example.blueprintai.data.DownloadProgress
import com.example.blueprintai.data.RemoteModelProfile
import com.example.blueprintai.data.Settings as AppSettings
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsDialog(
    logs: List<DiagnosticLog>,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
    modelStatus: String = "Initializing..."
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Diagnostic Logs")
                Text(
                    text = "System Status: $modelStatus",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                if (logs.isEmpty()) {
                    Text("No logs available.", modifier = Modifier.padding(16.dp))
                } else {
                    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
                    LazyColumn {
                        items(logs.reversed()) { log ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                val time = dateFormat.format(Date(log.timestamp))
                                Text(
                                    text = "[$time] ${log.level}/${log.tag}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (log.level == "ERROR") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Text(text = log.message, style = MaterialTheme.typography.bodySmall)
                                if (log.metadata.isNotEmpty()) {
                                    Text(
                                        text = log.metadata.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onExport) { Text("Export JSON") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup & Restore") },
        text = {
            Column {
                Text("Back up your conversations, attachments, and settings to a ZIP file, or restore from a previous backup.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateBackup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Full Backup (.zip)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRestoreBackup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore from ZIP")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AiModelsDialog(
    settings: AppSettings,
    remoteProfiles: List<RemoteModelProfile> = emptyList(),
    downloadProgress: DownloadProgress = DownloadProgress(),
    onDismiss: () -> Unit,
    onSaveSettings: (localPath: String, desktopUrl: String, geminiKey: String, isRemoteEnabled: Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onStartDownload: (String) -> Unit,
    onAddProfile: (label: String, localIp: String, publicIp: String, apiKey: String) -> Unit = { _, _, _, _ -> },
    onSelectProfile: (Long) -> Unit = {},
    onDeleteProfile: (RemoteModelProfile) -> Unit = {}
) {
    var localPath by remember(settings.localModelPath) { mutableStateOf(settings.localModelPath) }
    var desktopUrl by remember(settings.desktopUrl) { mutableStateOf(settings.desktopUrl) }
    var geminiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var isRemoteEnabled by remember(settings.isRemoteEnabled) { mutableStateOf(settings.isRemoteEnabled) }
    var showAddProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(downloadProgress.isCompleted) {
        if (downloadProgress.isCompleted) {
            localPath = "/storage/emulated/0/Download/AI_Models/gemma-4-E2B-it.litertlm"
            onSaveSettings(localPath, desktopUrl, geminiKey, isRemoteEnabled)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Model Configuration") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Local LiteRT-LM", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = localPath,
                    onValueChange = { localPath = it },
                    label = { Text("Model Path (.litertlm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Grant Permission", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Button(
                        onClick = {
                            onStartDownload("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm")
                        },
                        enabled = !downloadProgress.isDownloading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (downloadProgress.isDownloading) "Downloading..." else "Download Model",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (downloadProgress.isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress.progressFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val downloadedMb = downloadProgress.bytesDownloaded / (1024 * 1024)
                    val totalMb = downloadProgress.totalBytes / (1024 * 1024)
                    val pct = (downloadProgress.progressFraction * 100).toInt()
                    Text(
                        text = if (totalMb > 0) "$downloadedMb MB / $totalMb MB ($pct%)" else "$downloadedMb MB downloaded...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (downloadProgress.isCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "✅ Download complete! Saved to Download/AI_Models/",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }

                if (downloadProgress.error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "❌ Download error: ${downloadProgress.error}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Desktop OpenAI-compatible", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = isRemoteEnabled,
                        onCheckedChange = { isRemoteEnabled = it }
                    )
                }
                if (!isRemoteEnabled) {
                    Text(
                        "⏸️ Remote AI connection is currently suspended.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (remoteProfiles.isNotEmpty()) {
                    Text(
                        "Saved Remote Server Profiles",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    remoteProfiles.forEach { profile ->
                        Surface(
                            color = if (profile.isActive) Color(0xFF1E2638) else Color(0xFF181818),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = profile.isActive,
                                    onClick = { onSelectProfile(profile.id) }
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                    Text(
                                        text = profile.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Local: ${profile.localIpUrl}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    if (profile.publicIpUrl.isNotBlank()) {
                                        Text(
                                            text = "Public: ${profile.publicIpUrl}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteProfile(profile) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("✕", color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showAddProfileDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("+ Add Server Profile (Local & Public IP)")
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desktopUrl,
                    onValueChange = { desktopUrl = it },
                    label = { Text("Base URL (e.g. 192.168.1.10:1234)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "App will automatically append /v1 if missing",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Google Gemini API", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var cleaned = desktopUrl.trim()
                if (cleaned.isNotBlank()) {
                    if (!cleaned.startsWith("http://", ignoreCase = true) && !cleaned.startsWith("https://", ignoreCase = true)) {
                        cleaned = "http://$cleaned"
                    }
                    while (cleaned.endsWith("/", ignoreCase = true) || cleaned.endsWith("/v1", ignoreCase = true)) {
                        if (cleaned.endsWith("/", ignoreCase = true)) {
                            cleaned = cleaned.trimEnd('/')
                        } else if (cleaned.endsWith("/v1", ignoreCase = true)) {
                            cleaned = cleaned.substring(0, cleaned.length - 3)
                        }
                    }
                    cleaned = "$cleaned/v1"
                }
                onSaveSettings(localPath, cleaned, geminiKey, isRemoteEnabled)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showAddProfileDialog) {
        RemoteProfileDialog(
            onDismiss = { showAddProfileDialog = false },
            onConfirm = { label, localIp, publicIp, apiKey ->
                onAddProfile(label, localIp, publicIp, apiKey)
                showAddProfileDialog = false
            }
        )
    }
}

@Composable
fun RemoteProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, localIp: String, publicIp: String, apiKey: String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var localIp by remember { mutableStateOf("") }
    var publicIp by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Remote Model Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("e.g. Home Desktop (LM Studio)") },
                    label = { Text("Profile Name / Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = localIp,
                    onValueChange = { localIp = it },
                    placeholder = { Text("e.g. 192.168.1.50:1234") },
                    label = { Text("Local IP / Home Wi-Fi URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = publicIp,
                    onValueChange = { publicIp = it },
                    placeholder = { Text("e.g. 73.120.10.5:1234 or LM Link") },
                    label = { Text("Public IP / Away URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        label.ifBlank { "Desktop Server" },
                        localIp.trim(),
                        publicIp.trim(),
                        apiKey.trim()
                    )
                },
                enabled = localIp.isNotBlank() || publicIp.isNotBlank()
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
