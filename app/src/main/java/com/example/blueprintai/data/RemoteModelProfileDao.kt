package com.example.blueprintai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteModelProfileDao {
    @Query("SELECT * FROM remote_model_profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<RemoteModelProfile>>

    @Query("SELECT * FROM remote_model_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): RemoteModelProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: RemoteModelProfile): Long

    @Update
    suspend fun updateProfile(profile: RemoteModelProfile)

    @Delete
    suspend fun deleteProfile(profile: RemoteModelProfile)

    @Query("UPDATE remote_model_profiles SET isActive = 0")
    suspend fun clearActiveProfiles()

    @Query("UPDATE remote_model_profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun setActiveProfileById(profileId: Long)

    @Transaction
    suspend fun switchActiveProfile(profileId: Long) {
        clearActiveProfiles()
        setActiveProfileById(profileId)
    }
}
