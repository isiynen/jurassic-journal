package com.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.user.entity.UserDnaInventory

@Dao
interface UserDnaInventoryDao {
    @Query("SELECT * FROM user_dna_inventory WHERE dinoId = :dinoId")
    suspend fun get(dinoId: Long): UserDnaInventory?

    @Query("SELECT * FROM user_dna_inventory WHERE dinoId IN (:dinoIds)")
    suspend fun getForDinos(dinoIds: List<Long>): List<UserDnaInventory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: UserDnaInventory)
}
