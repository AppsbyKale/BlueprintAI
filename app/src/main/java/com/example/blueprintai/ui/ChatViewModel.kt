package com.example.blueprintai.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueprintai.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val folderRepository: FolderRepository,
    private val attachmentManager: AttachmentManager,
    private val voiceManager: VoiceManager,
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val folders: StateFlow<List<Folder>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) folderRepository.getFolders()
            else folderRepository.searchFolders(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<Message>> = combine(_currentFolderId, _searchQuery) { id, query ->
        id to query
    }.flatMapLatest { (id, query) ->
        when {
            id == null -> flowOf(emptyList())
            query.isBlank() -> repository.getMessages(id)
            else -> repository.searchMessagesInFolder(id, query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attachments: StateFlow<List<Attachment>> = _currentFolderId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList<Attachment>())
            else repository.getAttachmentsForFolder(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _currentStreamingResponse = MutableStateFlow("")
    val currentStreamingResponse: StateFlow<String> = _currentStreamingResponse.asStateFlow()

    private val _conceptExplanation = MutableStateFlow<String?>(null)
    val conceptExplanation: StateFlow<String?> = _conceptExplanation.asStateFlow()

    private val _isExplainingConcepts = MutableStateFlow(false)
    val isExplainingConcepts: StateFlow<Boolean> = _isExplainingConcepts.asStateFlow()

    val isListening = voiceManager.isListening
    val recognizedText = voiceManager.recognizedText

    private val settings = settingsDao.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    fun selectFolder(id: Long) {
        _currentFolderId.value = id
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val id = folderRepository.createFolder(name)
            _currentFolderId.value = id
        }
    }

    fun updateFolder(id: Long, name: String) {
        viewModelScope.launch {
            folderRepository.updateFolder(id, name)
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folder)
            if (_currentFolderId.value == folder.id) {
                _currentFolderId.value = null
            }
        }
    }

    fun mergeFolders(sourceFolderId: Long, targetFolderId: Long) {
        viewModelScope.launch {
            folderRepository.mergeFolders(sourceFolderId, targetFolderId)
            if (_currentFolderId.value == sourceFolderId) {
                _currentFolderId.value = targetFolderId
            }
        }
    }

    fun sendMessage(content: String) {
        val folderId = _currentFolderId.value ?: return
        if (content.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            _isGenerating.value = true
            _currentStreamingResponse.value = ""
            
            repository.sendMessage(folderId, content).collect { chunk ->
                _currentStreamingResponse.value += chunk
            }
            
            if (settings.value?.isTtsEnabled == true) {
                voiceManager.speak(_currentStreamingResponse.value)
            }
            
            _isGenerating.value = false
            _currentStreamingResponse.value = ""
        }
    }

    fun addAttachment(uri: Uri, type: String, name: String) {
        val folderId = _currentFolderId.value ?: return
        viewModelScope.launch {
            val extractedText = attachmentManager.extractText(uri, type)
            val aiSuggestedName = repository.generateSuggestedName(name, extractedText)
            
            val attachment = Attachment(
                folderId = folderId,
                originalName = name,
                aiSuggestedName = aiSuggestedName,
                type = type,
                extractedText = extractedText,
                uri = uri.toString()
            )
            repository.addAttachment(attachment)
        }
    }

    fun toggleKeyDecision(message: Message) {
        viewModelScope.launch {
            repository.updateMessageMetadata(message.id, !message.isKeyDecision, message.tags)
        }
    }

    fun updateTags(message: Message, tags: String) {
        viewModelScope.launch {
            repository.updateMessageMetadata(message.id, message.isKeyDecision, tags)
        }
    }

    fun explainConcepts(message: Message) {
        viewModelScope.launch {
            _isExplainingConcepts.value = true
            _conceptExplanation.value = null
            val explanation = repository.explainConcepts(message.content)
            _conceptExplanation.value = explanation
            _isExplainingConcepts.value = false
        }
    }

    fun clearConceptExplanation() {
        _conceptExplanation.value = null
        _isExplainingConcepts.value = false
    }

    fun startListening() {
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
