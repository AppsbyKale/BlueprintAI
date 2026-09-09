package com.example.blueprintai.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Folder::class, FolderFts::class,
        Message::class, MessageFts::class,
        Attachment::class, AttachmentFts::class,
        Settings::class,
        RemoteModelProfile::class
    ],
    version = 5
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun settingsDao(): SettingsDao
    abstract fun remoteModelProfileDao(): RemoteModelProfileDao
}
