package com.example.blueprintai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueprintai.data.ArtifactRepository
import com.example.blueprintai.data.ChatRepository
import com.example.blueprintai.data.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtifactExportState(
    val report: String = "",
    val blueprint: String = "",
    val conceptMap: String = "",
    val prompt: String = "",
    val tasks: String = "",
    val conversation: String = "",
    val isGenerating: Boolean = false
)

@HiltViewModel
class ArtifactViewModel @Inject constructor(
    private val artifactRepository: ArtifactRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _exportState = MutableStateFlow(ArtifactExportState())
    val exportState: StateFlow<ArtifactExportState> = _exportState.asStateFlow()

    fun generateArtifacts(folderId: Long) {
        viewModelScope.launch {
            _exportState.value = _exportState.value.copy(isGenerating = true)
            
            val report = artifactRepository.generateReport(folderId)
            val blueprint = artifactRepository.generateBlueprintUpdate(folderId)
            val conceptMap = artifactRepository.generateConceptMap(folderId)
            val prompt = artifactRepository.generatePromptExport(folderId)
            
            // Get messages for conversation and tasks
            val messages = chatRepository.getMessages(folderId).first()
            val conversation = messages.joinToString("\n\n") { "${it.role.uppercase()}: ${it.content}" }
            
            _exportState.value = ArtifactExportState(
                report = report,
                blueprint = blueprint,
                conceptMap = conceptMap,
                prompt = prompt,
                conversation = conversation,
                isGenerating = false
            )
        }
    }

    fun clearState() {
        _exportState.value = ArtifactExportState()
    }
}
