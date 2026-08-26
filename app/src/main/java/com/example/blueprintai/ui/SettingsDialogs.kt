package com.example.blueprintai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.blueprintai.data.DiagnosticLog
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
                    LazyColumn {
                        items(logs.reversed()) { log ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))
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
    onDismiss: () -> Unit,
    onUpdateLocalPath: (String) -> Unit,
    onUpdateDesktopUrl: (String) -> Unit,
    onUpdateGeminiKey: (String) -> Unit,
    onRequestPermission: () -> Unit
) {
    var localPath by remember { mutableStateOf(settings.localModelPath) }
    var desktopUrl by remember { mutableStateOf(settings.desktopUrl) }
    var geminiKey by remember { mutableStateOf(settings.geminiApiKey) }

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
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Grant All Files Permission")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Desktop OpenAI-compatible", style = MaterialTheme.typography.titleSmall)
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
                onUpdateLocalPath(localPath)
                val finalUrl = if (desktopUrl.startsWith("http")) desktopUrl else "http://$desktopUrl"
                val withV1 = if (finalUrl.endsWith("/v1")) finalUrl else "$finalUrl/v1"
                onUpdateDesktopUrl(withV1)
                onUpdateGeminiKey(geminiKey)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
