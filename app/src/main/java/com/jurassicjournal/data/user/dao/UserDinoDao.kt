package com.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.user.entity.UserDino

@Dao
interface UserDinoDao {
    @Query("SELECT * FROM user_dinos WHERE profileId = :profileId AND dinoId = :dinoId")
    suspend fun getByDinoId(profileId: Long, dinoId: Long): UserDino?

    @Query("SELECT * FROM user_dinos WHERE profileId = :profileId")
    suspend fun getForProfile(profileId: Long): List<UserDino>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(userDino: UserDino)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<UserDino>)
}
