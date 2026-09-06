package com.example.blueprintai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueprintai.data.BackupManager
import com.example.blueprintai.data.LogManager
import com.example.blueprintai.data.ModelDownloader
import com.example.blueprintai.data.Settings
import com.example.blueprintai.data.SettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val backupManager: BackupManager,
    private val logManager: LogManager,
    private val modelDownloader: ModelDownloader
) : ViewModel() {

    val settings: StateFlow<Settings> = settingsDao.getSettings()
        .map { it ?: Settings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    val downloadProgress = modelDownloader.downloadProgress

    val logs = logManager.logs

    val modelStatus: StateFlow<String> = logs.map { list ->
        list.lastOrNull { it.tag == "Model" || it.tag == "Attachment" }?.message ?: "System Ready"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Initializing...")

    fun updateModelMode(mode: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(modelMode = mode))
        }
    }

    fun toggleRemoteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(isRemoteEnabled = enabled))
        }
    }

    fun saveAiModelSettings(localPath: String, desktopUrl: String, geminiKey: String, isRemoteEnabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(
                current.copy(
                    localModelPath = localPath,
                    desktopUrl = desktopUrl,
                    geminiApiKey = geminiKey,
                    isRemoteEnabled = isRemoteEnabled
                )
            )
        }
    }

    fun updateLocalPath(path: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(localModelPath = path))
        }
    }

    fun updateDesktopUrl(url: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(desktopUrl = url))
        }
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(geminiApiKey = key))
        }
    }

    fun toggleTts(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsDao.saveSettings(current.copy(isTtsEnabled = enabled))
        }
    }

    fun createBackup(file: File) {
        viewModelScope.launch {
            backupManager.createBackup(file)
        }
    }

    fun restoreBackup(file: File) {
        viewModelScope.launch {
            backupManager.restoreBackup(file)
        }
    }

    fun clearLogs() {
        logManager.clearLogs()
    }

    fun exportLogs(): String = logManager.exportTrainingData()

    fun downloadGemmaModel(
        url: String = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        targetPath: String = "/storage/emulated/0/Download/AI_Models/gemma-4-E2B-it.litertlm"
    ) {
        viewModelScope.launch {
            modelDownloader.downloadModel(url, targetPath)
            if (modelDownloader.downloadProgress.value.isCompleted) {
                updateLocalPath(targetPath)
            }
        }
    }

    fun resetDownloadProgress() {
        modelDownloader.resetProgress()
    }
}
