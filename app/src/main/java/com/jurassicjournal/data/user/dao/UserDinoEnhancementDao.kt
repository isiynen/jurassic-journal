package com.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jurassicjournal.data.user.entity.UserDinoEnhancement

@Dao
interface UserDinoEnhancementDao {
    @Query("SELECT * FROM user_dino_enhancements WHERE profileId = :profileId AND dinoId = :dinoId")
    suspend fun getForDino(profileId: Long, dinoId: Long): List<UserDinoEnhancement>

    @Upsert
    suspend fun upsert(enhancement: UserDinoEnhancement)

    @Upsert
    suspend fun upsertAll(enhancements: List<UserDinoEnhancement>)
}
