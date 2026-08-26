package com.example.blueprintai.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao
) {
    fun getFolders(): Flow<List<Folder>> = folderDao.getAllFolders()

    fun searchFolders(query: String): Flow<List<Folder>> = folderDao.searchFolders(query)

    suspend fun createFolder(name: String): Long {
        return folderDao.insertFolder(Folder(name = name))
    }

    suspend fun updateFolder(id: Long, name: String) {
        folderDao.updateFolderName(id, name)
    }

    suspend fun deleteFolder(folder: Folder) {
        messageDao.deleteMessagesByFolder(folder.id)
        folderDao.deleteFolder(folder)
    }

    suspend fun mergeFolders(sourceFolderId: Long, targetFolderId: Long) {
        messageDao.moveMessagesToFolder(sourceFolderId, targetFolderId)
        attachmentDao.moveAttachmentsToFolder(sourceFolderId, targetFolderId)
        val sourceFolder = folderDao.getFolderById(sourceFolderId)
        if (sourceFolder != null) {
            folderDao.deleteFolder(sourceFolder)
        }
    }
}
