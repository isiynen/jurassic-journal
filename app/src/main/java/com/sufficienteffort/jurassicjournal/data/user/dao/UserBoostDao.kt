package com.sufficienteffort.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.user.entity.UserBoost
import kotlinx.coroutines.flow.Flow

@Dao
interface UserBoostDao {
    @Query("SELECT * FROM user_boosts WHERE profileId = :profileId AND dinoId = :dinoId")
    suspend fun getForDino(profileId: Long, dinoId: Long): List<UserBoost>

    @Query("SELECT * FROM user_boosts WHERE profileId = :profileId")
    suspend fun getForProfile(profileId: Long): List<UserBoost>

    @Query("SELECT * FROM user_boosts WHERE profileId = :profileId")
    fun observeForProfile(profileId: Long): Flow<List<UserBoost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(boost: UserBoost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<UserBoost>)
}
