package com.example.blueprintai.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY updatedAt DESC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Query("UPDATE folders SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFolderName(id: Long, name: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("""
        SELECT folders.* FROM folders
        JOIN folders_fts ON folders.name = folders_fts.name
        WHERE folders_fts MATCH :query
    """)
    fun searchFolders(query: String): Flow<List<Folder>>
}
