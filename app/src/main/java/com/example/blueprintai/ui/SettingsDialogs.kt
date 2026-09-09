package com.example.blueprintai.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.blueprintai.data.DiagnosticLog
import com.example.blueprintai.data.DownloadProgress
import com.example.blueprintai.data.RemoteModelProfile
import com.example.blueprintai.data.Settings as AppSettings
import java.text.SimpleDateFormat
import java.util.*

fun cleanDesktopUrl(input: String): String {
    var url = input.trim()
    if (url.isEmpty()) return ""

    if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
        url = "http://$url"
    }

    while (url.endsWith("/", ignoreCase = true) || url.endsWith("/v1", ignoreCase = true)) {
        if (url.endsWith("/", ignoreCase = true)) {
            url = url.trimEnd('/')
        } else if (url.endsWith("/v1", ignoreCase = true)) {
            url = url.substring(0, url.length - 3)
        }
    }

    return "$url/v1"
}

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
    onSaveSettings: (localPath: String, geminiKey: String, isRemoteEnabled: Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onStartDownload: (String) -> Unit,
    onAddProfile: (label: String, localIp: String, publicIp: String, apiKey: String) -> Unit = { _, _, _, _ -> },
    onUpdateProfile: (RemoteModelProfile) -> Unit = {},
    onSelectProfile: (Long) -> Unit = {},
    onSetProfileIpMode: (Long, String) -> Unit = { _, _ -> },
    onDeleteProfile: (RemoteModelProfile) -> Unit = {}
) {
    var localPath by remember(settings.localModelPath) { mutableStateOf(settings.localModelPath) }
    var geminiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var isRemoteEnabled by remember(settings.isRemoteEnabled) { mutableStateOf(settings.isRemoteEnabled) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<RemoteModelProfile?>(null) }
    var activeProfileDropdownExpanded by remember { mutableStateOf(false) }

    val activeProfile = remoteProfiles.find { it.isActive }

    LaunchedEffect(downloadProgress.isCompleted) {
        if (downloadProgress.isCompleted) {
            localPath = "/storage/emulated/0/Download/AI_Models/gemma-4-E2B-it.litertlm"
            onSaveSettings(localPath, geminiKey, isRemoteEnabled)
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
                        "Download complete. Saved to Download/AI_Models/",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }

                if (downloadProgress.error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Download error: ${downloadProgress.error}",
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
                        "Remote AI connection is currently suspended.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    "Active Remote Server Profile",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { activeProfileDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeProfile?.label ?: "No Remote Profile Selected",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                                if (activeProfile != null) {
                                    Text(
                                        text = "Local: ${activeProfile.localIpUrl}${if (activeProfile.publicIpUrl.isNotBlank()) " | Public: ${activeProfile.publicIpUrl}" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Text("▼", color = Color.Gray)
                        }
                    }

                    DropdownMenu(
                        expanded = activeProfileDropdownExpanded,
                        onDismissRequest = { activeProfileDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        remoteProfiles.forEach { profile ->
                            var showItemMenu by remember { mutableStateOf(false) }

                            Box {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (profile.isActive) "✓ ${profile.label}" else profile.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (profile.isActive) MaterialTheme.colorScheme.primary else Color.White
                                                )
                                                Text(
                                                    text = "Local: ${profile.localIpUrl}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray
                                                )
                                            }
                                            IconButton(
                                                onClick = { showItemMenu = true },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.Gray)
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectProfile(profile.id)
                                        activeProfileDropdownExpanded = false
                                    },
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { showItemMenu = true },
                                            onTap = {
                                                onSelectProfile(profile.id)
                                                activeProfileDropdownExpanded = false
                                            }
                                        )
                                    }
                                )

                                DropdownMenu(
                                    expanded = showItemMenu,
                                    onDismissRequest = { showItemMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Profile") },
                                        onClick = {
                                            profileToEdit = profile
                                            showItemMenu = false
                                            activeProfileDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Profile", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            onDeleteProfile(profile)
                                            showItemMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (activeProfile != null && activeProfile.publicIpUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active IP:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        
                        FilterChip(
                            selected = activeProfile.activeIpMode == "LOCAL",
                            onClick = { onSetProfileIpMode(activeProfile.id, "LOCAL") },
                            label = { Text("Local IP (Wi-Fi)", style = MaterialTheme.typography.labelSmall) }
                        )
                        
                        FilterChip(
                            selected = activeProfile.activeIpMode == "PUBLIC",
                            onClick = { onSetProfileIpMode(activeProfile.id, "PUBLIC") },
                            label = { Text("Public IP (Away)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showAddProfileDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("+ Add Server Profile (Local & Public IP)")
                }
                
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
                onSaveSettings(localPath, geminiKey, isRemoteEnabled)
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

    profileToEdit?.let { profile ->
        RemoteProfileDialog(
            initialProfile = profile,
            onDismiss = { profileToEdit = null },
            onConfirm = { label, localIp, publicIp, apiKey ->
                onUpdateProfile(
                    profile.copy(
                        label = label,
                        localIpUrl = localIp,
                        publicIpUrl = publicIp,
                        apiKey = apiKey
                    )
                )
                profileToEdit = null
            }
        )
    }
}

@Composable
fun RemoteProfileDialog(
    initialProfile: RemoteModelProfile? = null,
    onDismiss: () -> Unit,
    onConfirm: (label: String, localIp: String, publicIp: String, apiKey: String) -> Unit
) {
    var label by remember(initialProfile) { mutableStateOf(initialProfile?.label ?: "") }
    var localIp by remember(initialProfile) { mutableStateOf(initialProfile?.localIpUrl ?: "") }
    var publicIp by remember(initialProfile) { mutableStateOf(initialProfile?.publicIpUrl ?: "") }
    var apiKey by remember(initialProfile) { mutableStateOf(initialProfile?.apiKey ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProfile == null) "Add Remote Model Profile" else "Edit Remote Model Profile") },
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
