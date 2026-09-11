package com.example.blueprintai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueprintai.data.ArtifactExporter
import com.example.blueprintai.data.ArtifactRepository
import com.example.blueprintai.domain.repository.IChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.charset.StandardCharsets
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
    private val chatRepository: IChatRepository,
    private val artifactExporter: ArtifactExporter
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

    suspend fun writeExportToStream(
        outputStream: OutputStream,
        selectedOptions: Set<String>,
        format: String
    ) = withContext(Dispatchers.IO) {
        outputStream.use { out ->
            if (format == "zip") {
                val zipBytes = artifactExporter.createZipArchive(_exportState.value, selectedOptions)
                out.write(zipBytes)
            } else {
                val documentText = artifactExporter.formatMergedDocument(_exportState.value, selectedOptions)
                out.write(documentText.toByteArray(StandardCharsets.UTF_8))
            }
            out.flush()
        }
    }

    fun clearState() {
        _exportState.value = ArtifactExportState()
    }
}
