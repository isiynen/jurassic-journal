package com.sufficienteffort.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDnaInventory

@Dao
interface UserDnaInventoryDao {
    @Query("SELECT * FROM user_dna_inventory WHERE profileId = :profileId AND dinoId = :dinoId")
    suspend fun get(profileId: Long, dinoId: Long): UserDnaInventory?

    @Query("SELECT * FROM user_dna_inventory WHERE profileId = :profileId AND dinoId IN (:dinoIds)")
    suspend fun getForDinos(profileId: Long, dinoIds: List<Long>): List<UserDnaInventory>

    @Query("SELECT * FROM user_dna_inventory WHERE profileId = :profileId")
    suspend fun getForProfile(profileId: Long): List<UserDnaInventory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: UserDnaInventory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<UserDnaInventory>)
}
