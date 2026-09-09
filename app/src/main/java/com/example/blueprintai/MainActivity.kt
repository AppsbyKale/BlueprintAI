package com.example.blueprintai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.example.blueprintai.ui.ChatScreen
import com.example.blueprintai.ui.ChatViewModel
import com.example.blueprintai.ui.SidebarContent
import com.example.blueprintai.ui.SettingsViewModel
import com.example.blueprintai.ui.ArtifactViewModel
import com.example.blueprintai.ui.ExportDialog
import com.example.blueprintai.ui.theme.BlueprintAITheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.blueprintai.ui.*
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlueprintAITheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    chatViewModel: ChatViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    artifactViewModel: ArtifactViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAiModelsDialog by remember { mutableStateOf(false) }

    val settings by settingsViewModel.settings.collectAsState()
    val logs by settingsViewModel.logs.collectAsState()
    val modelStatus by settingsViewModel.modelStatus.collectAsState()
    val exportState by artifactViewModel.exportState.collectAsState()
    val currentFolderId by chatViewModel.currentFolderId.collectAsState()
    val downloadProgress by settingsViewModel.downloadProgress.collectAsState()
    val remoteProfiles by settingsViewModel.remoteProfiles.collectAsState()

    val folders by chatViewModel.folders.collectAsState()
    val context = LocalContext.current

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let {
                val tempFile = File(context.cacheDir, "backup.zip")
                settingsViewModel.createBackup(tempFile)
                // In a real app, copy tempFile to uri. Here we mock it for brevity.
                context.contentResolver.openOutputStream(it)?.use { out ->
                    tempFile.inputStream().copyTo(out)
                }
            }
        }
    )

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val tempFile = File(context.cacheDir, "restore.zip")
                context.contentResolver.openInputStream(it)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                settingsViewModel.restoreBackup(tempFile)
            }
        }
    )

    LaunchedEffect(folders) {
        if (chatViewModel.currentFolderId.value == null && folders.isNotEmpty()) {
            chatViewModel.selectFolder(folders.first().id)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                SidebarContent(
                    viewModel = chatViewModel,
                    onFolderClick = { 
                        chatViewModel.selectFolder(it)
                        scope.launch { drawerState.close() }
                    },
                    onExportClick = { folder ->
                        artifactViewModel.generateArtifacts(folder.id)
                        showExportDialog = true
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            listOf("Auto", "Desktop", "Phone").forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = settings.modelMode == mode,
                                    onClick = { settingsViewModel.updateModelMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                    label = { Text(mode, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showSettingsMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                            }
                            DropdownMenu(
                                expanded = showSettingsMenu,
                                onDismissRequest = { showSettingsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("AI Models") },
                                    onClick = { 
                                        showAiModelsDialog = true
                                        showSettingsMenu = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Backup & Restore") },
                                    onClick = { 
                                        showBackupDialog = true
                                        showSettingsMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("View Logs") },
                                    onClick = { 
                                        showLogsDialog = true
                                        showSettingsMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Remote Desktop AI")
                                            Spacer(Modifier.weight(1f))
                                            Switch(
                                                checked = settings.isRemoteEnabled,
                                                onCheckedChange = { settingsViewModel.toggleRemoteEnabled(it) }
                                            )
                                        }
                                    },
                                    onClick = { settingsViewModel.toggleRemoteEnabled(!settings.isRemoteEnabled) }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Audio Readback")
                                            Spacer(Modifier.weight(1f))
                                            Switch(
                                                checked = settings.isTtsEnabled,
                                                onCheckedChange = { settingsViewModel.toggleTts(it) }
                                            )
                                        }
                                    },
                                    onClick = { settingsViewModel.toggleTts(!settings.isTtsEnabled) }
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            ChatScreen(
                modifier = Modifier.padding(
                    top = innerPadding.calculateTopPadding()
                ),
                viewModel = chatViewModel
            )
        }
    }

    if (showExportDialog) {
        ExportDialog(
            state = exportState,
            onDismiss = { 
                showExportDialog = false
                artifactViewModel.clearState()
            },
            onExport = { selected, format ->
                val textToShare = StringBuilder()
                if (selected.contains("Report")) textToShare.append("# Report\n${exportState.report}\n\n")
                if (selected.contains("Blueprint")) textToShare.append("# Blueprint\n${exportState.blueprint}\n\n")
                if (selected.contains("Concept Map")) textToShare.append("# Concept Map & Architecture\n${exportState.conceptMap}\n\n")
                if (selected.contains("Prompt")) textToShare.append("# Prompt\n${exportState.prompt}\n\n")
                if (selected.contains("Conversation")) textToShare.append("# Conversation\n${exportState.conversation}\n\n")
                
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, textToShare.toString())
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share Artifacts"))
                showExportDialog = false
            }
        )
    }

    if (showLogsDialog) {
        LogsDialog(
            logs = logs,
            onDismiss = { showLogsDialog = false },
            onExport = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, settingsViewModel.exportLogs())
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Export Logs"))
            },
            onClear = { settingsViewModel.clearLogs() },
            modelStatus = modelStatus
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            onCreateBackup = { 
                createBackupLauncher.launch("blueprint_backup_${System.currentTimeMillis()}.zip")
                showBackupDialog = false
            },
            onRestoreBackup = {
                restoreBackupLauncher.launch(arrayOf("application/zip"))
                showBackupDialog = false
            }
        )
    }

    if (showAiModelsDialog) {
        AiModelsDialog(
            settings = settings,
            remoteProfiles = remoteProfiles,
            downloadProgress = downloadProgress,
            onDismiss = { showAiModelsDialog = false },
            onSaveSettings = { localPath, desktopUrl, geminiKey, isRemoteEnabled ->
                settingsViewModel.saveAiModelSettings(localPath, desktopUrl, geminiKey, isRemoteEnabled)
            },
            onRequestPermission = {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            },
            onStartDownload = { url ->
                settingsViewModel.downloadGemmaModel(url)
            },
            onAddProfile = { label, localIp, publicIp, apiKey ->
                settingsViewModel.addRemoteProfile(label, localIp, publicIp, apiKey)
            },
            onSelectProfile = { profileId ->
                settingsViewModel.setActiveRemoteProfile(profileId)
            },
            onDeleteProfile = { profile ->
                settingsViewModel.deleteRemoteProfile(profile)
            }
        )
    }
}

