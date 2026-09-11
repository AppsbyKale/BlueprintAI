package com.example.blueprintai.domain.repository

import com.example.blueprintai.data.Folder
import kotlinx.coroutines.flow.Flow

interface IFolderRepository {
    fun getFolders(): Flow<List<Folder>>
    fun searchFolders(query: String): Flow<List<Folder>>
    suspend fun createFolder(name: String): Long
    suspend fun updateFolder(id: Long, name: String)
    suspend fun deleteFolder(folder: Folder)
    suspend fun mergeFolders(sourceFolderId: Long, targetFolderId: Long)
}
