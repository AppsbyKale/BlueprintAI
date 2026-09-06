package com.example.blueprintai.data

import com.example.blueprintai.model.ChatMessage
import com.example.blueprintai.model.ModelManager
import com.example.blueprintai.model.ToolInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
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
    // Cache for older conversation summaries per folder: folderId -> Pair(olderMessageCount, summaryText)
    private val folderSummaries = ConcurrentHashMap<Long, Pair<Int, String>>()

    fun getMessages(folderId: Long): Flow<List<Message>> = messageDao.getMessagesByFolder(folderId)

    fun searchMessages(query: String): Flow<List<Message>> = messageDao.searchMessages(query)

    fun searchMessagesInFolder(folderId: Long, query: String): Flow<List<Message>> = 
        messageDao.searchMessagesInFolder(folderId, query)

    suspend fun sendMessage(folderId: Long, content: String): Flow<String> = flow {
        // Save user message
        val userMessage = Message(folderId = folderId, content = content, role = "user")
        messageDao.insertMessage(userMessage)

        // Load all stored messages for this folder
        val allMessages = messageDao.getMessagesByFolder(folderId).first()

        // Build efficient chat payload using sliding window (last 5 messages verbatim + rolling summary for older history)
        var currentHistory = buildCompressedChatHistory(folderId, allMessages)

        // Get active model client (Local, Desktop, or Gemini)
        val client = modelManager.getActiveClient()

        var loop = true
        var interactionCount = 0

        while (loop && interactionCount < 5) {
            val fullResponse = StringBuilder()
            client.generateChatResponse(currentHistory).collect { chunk ->
                fullResponse.append(chunk)
                emit(chunk)
            }

            val responseText = fullResponse.toString().trim()
            if (responseText.startsWith("{") && responseText.endsWith("}")) {
                // Potential tool call
                val toolResult = toolInterceptor.intercept(responseText)
                emit("\n[Tool Result: $toolResult]\n")
                currentHistory = currentHistory + ChatMessage(role = "assistant", content = responseText) + ChatMessage(role = "user", content = "Tool result: $toolResult")
                interactionCount++
            } else {
                // Normal response
                val aiMessage = Message(folderId = folderId, content = responseText, role = "assistant")
                messageDao.insertMessage(aiMessage)
                loop = false
            }
        }
    }

    private suspend fun buildCompressedChatHistory(folderId: Long, allMessages: List<Message>): List<ChatMessage> {
        val windowSize = 5
        if (allMessages.size <= windowSize) {
            // Under 5 messages: send all verbatim, no summary needed
            return allMessages.map { ChatMessage(role = it.role, content = it.content) }
        }

        // More than 5 messages: split into older history and last 5 messages
        val olderMessages = allMessages.dropLast(windowSize)
        val recentMessages = allMessages.takeLast(windowSize)

        val cachedSummary = folderSummaries[folderId]
        val summaryText = if (cachedSummary != null && cachedSummary.first == olderMessages.size) {
            cachedSummary.second
        } else {
            val olderContentText = olderMessages.joinToString("\n") { "${it.role.uppercase()}: ${it.content}" }
            val newSummary = generateQuickSummary(olderContentText)
            folderSummaries[folderId] = Pair(olderMessages.size, newSummary)
            newSummary
        }

        val systemSummaryMessage = ChatMessage(
            role = "system",
            content = "Summary of prior conversation history:\n$summaryText"
        )

        return listOf(systemSummaryMessage) + recentMessages.map { ChatMessage(role = it.role, content = it.content) }
    }

    private suspend fun generateQuickSummary(text: String): String {
        return try {
            val client = modelManager.getActiveClient()
            val prompt = "Summarize the key facts, user goals, and technical decisions in this prior conversation in 2-3 concise bullet points:\n$text"
            val response = StringBuilder()
            client.generateResponse(prompt).collect { response.append(it) }
            response.toString().trim().takeIf { it.isNotEmpty() }
                ?: "Prior conversation covered ${text.length} characters of discussion."
        } catch (e: Exception) {
            "Prior conversation covered ${text.length} characters of discussion."
        }
    }

    suspend fun updateMessageMetadata(messageId: Long, isKey: Boolean, tags: String) {
        messageDao.updateMessageMetadata(messageId, isKey, tags)
    }

    suspend fun explainConcepts(messageContent: String): String {
        return try {
            val client = modelManager.getActiveClient()
            val prompt = """
                Analyze the following text/code snippet and explain it for a beginner software builder (a visual learner).
                
                Format your response clearly into 3 distinct sections:
                
                1. KEY CONCEPTS & TERMS
                (Define 2-4 key technical terms or keywords mentioned in plain English with simple analogies).
                
                2. RELATIONSHIPS & CAUSE-AND-EFFECT
                (Explain how the components interact. E.g., "If you change X, it affects Y").
                
                3. VISUAL FLOW / DIAGRAM
                (Use simple text/ASCII boxes or step-by-step arrows to show the flow of data or execution).
                
                Snippet to Explain:
                $messageContent
            """.trimIndent()

            val response = StringBuilder()
            client.generateResponse(prompt).collect { response.append(it) }
            response.toString()
        } catch (e: Throwable) {
            "Unable to generate concept explanation: ${e.localizedMessage ?: "Unknown error"}"
        }
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
