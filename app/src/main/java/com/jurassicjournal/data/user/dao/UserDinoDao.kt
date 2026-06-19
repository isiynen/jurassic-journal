package com.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.user.entity.UserDino

@Dao
interface UserDinoDao {
    @Query("SELECT * FROM user_dinos WHERE dinoId = :dinoId")
    suspend fun getByDinoId(dinoId: Long): UserDino?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(userDino: UserDino)
}
