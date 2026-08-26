package com.example.blueprintai.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    fun getAttachmentsForMessage(messageId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE folderId = :folderId")
    fun getAttachmentsForFolder(folderId: Long): Flow<List<Attachment>>

    @Query("UPDATE attachments SET folderId = :targetFolderId WHERE folderId = :sourceFolderId")
    suspend fun moveAttachmentsToFolder(sourceFolderId: Long, targetFolderId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment): Long

    @Delete
    suspend fun deleteAttachment(attachment: Attachment)

    @Query("""
        SELECT attachments.* FROM attachments
        JOIN attachments_fts ON attachments.rowid = attachments_fts.docid
        WHERE attachments_fts MATCH :query
    """)
    fun searchAttachments(query: String): Flow<List<Attachment>>
}
