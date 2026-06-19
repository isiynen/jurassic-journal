package com.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.user.entity.UserBoost

@Dao
interface UserBoostDao {
    @Query("SELECT * FROM user_boosts WHERE dinoId = :dinoId")
    suspend fun getForDino(dinoId: Long): List<UserBoost>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(boost: UserBoost)
}
