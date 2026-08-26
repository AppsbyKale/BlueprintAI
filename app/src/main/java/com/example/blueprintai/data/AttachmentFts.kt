package com.example.blueprintai.data

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = Attachment::class)
@Entity(tableName = "attachments_fts")
data class AttachmentFts(
    val originalName: String,
    val aiSuggestedName: String?,
    val extractedText: String?
)
