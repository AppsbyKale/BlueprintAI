package com.example.blueprintai.ui

import androidx.compose.runtime.Composable
import com.example.blueprintai.data.DiagnosticLog
import com.example.blueprintai.data.DownloadProgress
import com.example.blueprintai.data.RemoteModelProfile
import com.example.blueprintai.data.Settings as AppSettings
import com.example.blueprintai.ui.dialogs.AiModelsDialog as ComposableAiModelsDialog
import com.example.blueprintai.ui.dialogs.BackupDialog as ComposableBackupDialog
import com.example.blueprintai.ui.dialogs.LogsDialog as ComposableLogsDialog
import com.example.blueprintai.ui.dialogs.RemoteProfileDialog as ComposableRemoteProfileDialog

fun cleanDesktopUrl(input: String): String {
    var trimmed = input.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""

    trimmed = trimmed.removeSuffix("/chat/completions")
        .removeSuffix("/models")
        .trimEnd('/')

    var url = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
        "http://$trimmed"
    } else {
        trimmed
    }

    if (!url.endsWith("/v1", ignoreCase = true)) {
        url = "$url/v1"
    }

    return url
}

@Composable
fun LogsDialog(
    logs: List<DiagnosticLog>,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onClear: () -> Unit,
    modelStatus: String = "Initializing..."
) {
    ComposableLogsDialog(logs, onDismiss, onExport, onClear, modelStatus)
}

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    ComposableBackupDialog(onDismiss, onCreateBackup, onRestoreBackup)
}

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
    ComposableAiModelsDialog(
        settings = settings,
        remoteProfiles = remoteProfiles,
        downloadProgress = downloadProgress,
        onDismiss = onDismiss,
        onSaveSettings = onSaveSettings,
        onRequestPermission = onRequestPermission,
        onStartDownload = onStartDownload,
        onAddProfile = onAddProfile,
        onUpdateProfile = onUpdateProfile,
        onSelectProfile = onSelectProfile,
        onSetProfileIpMode = onSetProfileIpMode,
        onDeleteProfile = onDeleteProfile,
        onFetchModels = onFetchModels
    )
}

@Composable
fun RemoteProfileDialog(
    initialProfile: RemoteModelProfile? = null,
    onDismiss: () -> Unit,
    onConfirm: (label: String, localIp: String, publicIp: String, modelName: String, apiKey: String) -> Unit,
    onFetchModels: suspend (url: String, apiKey: String) -> List<String> = { _, _ -> emptyList() }
) {
    ComposableRemoteProfileDialog(
        initialProfile = initialProfile,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onFetchModels = onFetchModels
    )
}
