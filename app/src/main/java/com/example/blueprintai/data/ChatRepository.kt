package com.example.blueprintai.data

import com.example.blueprintai.model.ModelManager
import com.example.blueprintai.model.ToolInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val modelManager: ModelManager,
    private val toolInterceptor: ToolInterceptor
) {
    fun getMessages(folderId: Long): Flow<List<Message>> = messageDao.getMessagesByFolder(folderId)

    fun searchMessages(query: String): Flow<List<Message>> = messageDao.searchMessages(query)

    fun searchMessagesInFolder(folderId: Long, query: String): Flow<List<Message>> = 
        messageDao.searchMessagesInFolder(folderId, query)

    suspend fun sendMessage(folderId: Long, content: String): Flow<String> = flow {
        // Save user message
        val userMessage = Message(folderId = folderId, content = content, role = "user")
        messageDao.insertMessage(userMessage)

        // Get model client
        val client = modelManager.getActiveClient()

        var currentPrompt = content
        var loop = true
        var interactionCount = 0

        while (loop && interactionCount < 5) { // Limit loops
            val fullResponse = StringBuilder()
            client.generateResponse(currentPrompt).collect { chunk ->
                fullResponse.append(chunk)
                emit(chunk)
            }

            val responseText = fullResponse.toString().trim()
            if (responseText.startsWith("{") && responseText.endsWith("}")) {
                // Potential tool call
                val toolResult = toolInterceptor.intercept(responseText)
                emit("\n[Tool Result: $toolResult]\n")
                currentPrompt = "Tool result: $toolResult"
                interactionCount++
            } else {
                // Normal response
                val aiMessage = Message(folderId = folderId, content = responseText, role = "assistant")
                messageDao.insertMessage(aiMessage)
                loop = false
            }
        }
    }

    suspend fun updateMessageMetadata(messageId: Long, isKey: Boolean, tags: String) {
        messageDao.updateMessageMetadata(messageId, isKey, tags)
    }

    fun getAttachmentsForMessage(messageId: Long): Flow<List<Attachment>> = 
        attachmentDao.getAttachmentsForMessage(messageId)

    fun getAttachmentsForFolder(folderId: Long): Flow<List<Attachment>> = 
        attachmentDao.getAttachmentsForFolder(folderId)

    suspend fun addAttachment(attachment: Attachment) {
        attachmentDao.insertAttachment(attachment)
    }

    suspend fun generateSuggestedName(originalName: String, extractedText: String?): String {
        return try {
            val client = modelManager.getActiveClient()
            val prompt = """
                Suggest a very short, descriptive filename (max 5 words) for an attachment.
                Original name: $originalName
                Extracted text snippet: ${extractedText?.take(300) ?: "None"}
                Return ONLY the suggested name (slug-style or Title Case), no quotes or explanation.
            """.trimIndent()
            
            val response = StringBuilder()
            client.generateResponse(prompt).collect { response.append(it) }
            response.toString().trim().takeIf { it.isNotEmpty() } ?: "Analyzed_$originalName"
        } catch (e: Exception) {
            "Analyzed_$originalName"
        }
    }
}
