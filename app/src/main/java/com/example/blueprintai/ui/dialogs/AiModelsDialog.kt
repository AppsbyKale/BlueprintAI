package com.example.blueprintai.ui.dialogs

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.blueprintai.data.DownloadProgress
import com.example.blueprintai.data.RemoteModelProfile
import com.example.blueprintai.data.Settings as AppSettings
import com.example.blueprintai.ui.cleanDesktopUrl
import kotlinx.coroutines.launch

@Composable
fun AiModelsDialog(
    settings: AppSettings,
    remoteProfiles: List<RemoteModelProfile> = emptyList(),
    downloadProgress: DownloadProgress = DownloadProgress(),
    onDismiss: () -> Unit,
    onSaveSettings: (localPath: String, desktopUrl: String, geminiKey: String) -> Unit,
    onRequestPermission: () -> Unit,
    onStartDownload: (String) -> Unit,
    onAddProfile: (label: String, localIp: String, publicIp: String, modelName: String, apiKey: String) -> Unit = { _, _, _, _, _ -> },
    onUpdateProfile: (RemoteModelProfile) -> Unit = {},
    onSelectProfile: (Long) -> Unit = {},
    onSetProfileIpMode: (Long, String) -> Unit = { _, _ -> },
    onDeleteProfile: (RemoteModelProfile) -> Unit = {},
    onFetchModels: suspend (url: String, apiKey: String) -> List<String> = { _, _ -> emptyList() }
) {
    val activeProfile = remoteProfiles.find { it.isActive }
    var localPath by remember(settings.localModelPath) { mutableStateOf(settings.localModelPath) }
    var desktopUrl by remember(activeProfile?.localIpUrl, settings.desktopUrl) { mutableStateOf(activeProfile?.localIpUrl ?: settings.desktopUrl) }
    var geminiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<RemoteModelProfile?>(null) }

    LaunchedEffect(downloadProgress.isCompleted) {
        if (downloadProgress.isCompleted) {
            localPath = "/storage/emulated/0/Download/AI_Models/gemma-4-E2B-it.litertlm"
            onSaveSettings(localPath, desktopUrl, geminiKey)
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
                Text("Desktop Server Profiles", style = MaterialTheme.typography.titleSmall)

                if (remoteProfiles.isNotEmpty()) {
                    Text(
                        "Saved Remote Server Profiles (Tap to Select • Long Press or Edit to Change)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )

                    remoteProfiles.forEach { profile ->
                        Surface(
                            color = if (profile.isActive) Color(0xFF1E2638) else Color(0xFF181818),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .pointerInput(profile.id) {
                                    detectTapGestures(
                                        onTap = { onSelectProfile(profile.id) },
                                        onLongPress = { profileToEdit = profile }
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = profile.isActive,
                                    onClick = { onSelectProfile(profile.id) }
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                    Text(
                                        text = profile.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (profile.isActive) MaterialTheme.colorScheme.primary else Color.White
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
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = { profileToEdit = profile },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteProfile(profile) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
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
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desktopUrl,
                    onValueChange = { desktopUrl = it },
                    label = { Text("Active Server Local IP / URL") },
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
                onSaveSettings(localPath, cleanDesktopUrl(desktopUrl), geminiKey)
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
            onConfirm = { label, localIp, publicIp, modelName, apiKey ->
                onAddProfile(label, localIp, publicIp, modelName, apiKey)
                showAddProfileDialog = false
            },
            onFetchModels = onFetchModels
        )
    }

    profileToEdit?.let { profile ->
        RemoteProfileDialog(
            initialProfile = profile,
            onDismiss = { profileToEdit = null },
            onConfirm = { label, localIp, publicIp, modelName, apiKey ->
                onUpdateProfile(
                    profile.copy(
                        label = label,
                        localIpUrl = localIp,
                        publicIpUrl = publicIp,
                        modelName = modelName,
                        apiKey = apiKey
                    )
                )
                profileToEdit = null
            },
            onFetchModels = onFetchModels
        )
    }
}
