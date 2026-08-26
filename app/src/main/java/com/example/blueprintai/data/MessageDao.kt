package com.example.blueprintai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE folderId = :folderId ORDER BY timestamp ASC")
    fun getMessagesByFolder(folderId: Long): Flow<List<Message>>

    @Query("UPDATE messages SET folderId = :targetFolderId WHERE folderId = :sourceFolderId")
    suspend fun moveMessagesToFolder(sourceFolderId: Long, targetFolderId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("UPDATE messages SET isKeyDecision = :isKey, tags = :tags WHERE id = :id")
    suspend fun updateMessageMetadata(id: Long, isKey: Boolean, tags: String)

    @Query("DELETE FROM messages WHERE folderId = :folderId")
    suspend fun deleteMessagesByFolder(folderId: Long)

    @Query("""
        SELECT messages.* FROM messages
        JOIN messages_fts ON messages.content = messages_fts.content
        WHERE messages_fts MATCH :query
    """)
    fun searchMessages(query: String): Flow<List<Message>>

    @Query("""
        SELECT messages.* FROM messages
        JOIN messages_fts ON messages.content = messages_fts.content
        WHERE messages.folderId = :folderId AND messages_fts MATCH :query
    """)
    fun searchMessagesInFolder(folderId: Long, query: String): Flow<List<Message>>
}
