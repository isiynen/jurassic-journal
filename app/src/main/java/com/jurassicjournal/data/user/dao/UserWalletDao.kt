package com.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.user.entity.UserWallet

@Dao
interface UserWalletDao {
    @Query("SELECT * FROM user_wallet WHERE id = 1")
    suspend fun get(): UserWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: UserWallet)
}
