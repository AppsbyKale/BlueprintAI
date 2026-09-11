package com.example.blueprintai.domain.repository

import com.example.blueprintai.data.Attachment
import com.example.blueprintai.data.DebugContextInfo
import com.example.blueprintai.data.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IChatRepository {
    val currentDebugInfo: StateFlow<DebugContextInfo?>
    fun getDebugInfoForMessage(messageId: Long): DebugContextInfo?
    fun getMessages(folderId: Long): Flow<List<Message>>
    fun searchMessages(query: String): Flow<List<Message>>
    fun searchMessagesInFolder(folderId: Long, query: String): Flow<List<Message>>
    suspend fun sendMessage(folderId: Long, content: String): Flow<String>
    suspend fun updateMessageMetadata(messageId: Long, isKey: Boolean, tags: String)
    suspend fun explainConcepts(messageContent: String): String
    fun getAttachmentsForMessage(messageId: Long): Flow<List<Attachment>>
    fun getAttachmentsForFolder(folderId: Long): Flow<List<Attachment>>
    suspend fun addAttachment(attachment: Attachment)
    suspend fun generateSuggestedName(originalName: String, extractedText: String?): String
}
