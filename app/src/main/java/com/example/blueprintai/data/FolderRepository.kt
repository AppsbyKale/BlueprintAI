package com.example.blueprintai.data

import com.example.blueprintai.domain.repository.IFolderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao
) : IFolderRepository {
    override fun getFolders(): Flow<List<Folder>> = folderDao.getAllFolders()

    override fun searchFolders(query: String): Flow<List<Folder>> = folderDao.searchFolders(query)

    override suspend fun createFolder(name: String): Long {
        return folderDao.insertFolder(Folder(name = name))
    }

    override suspend fun updateFolder(id: Long, name: String) {
        folderDao.updateFolderName(id, name)
    }

    override suspend fun deleteFolder(folder: Folder) {
        messageDao.deleteMessagesByFolder(folder.id)
        folderDao.deleteFolder(folder)
    }

    override suspend fun mergeFolders(sourceFolderId: Long, targetFolderId: Long) {
        messageDao.moveMessagesToFolder(sourceFolderId, targetFolderId)
        attachmentDao.moveAttachmentsToFolder(sourceFolderId, targetFolderId)
        val sourceFolder = folderDao.getFolderById(sourceFolderId)
        if (sourceFolder != null) {
            folderDao.deleteFolder(sourceFolder)
        }
    }
}
