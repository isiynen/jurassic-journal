package com.sufficienteffort.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.user.entity.UserWallet

@Dao
interface UserWalletDao {
    @Query("SELECT * FROM user_wallet WHERE profileId = :profileId")
    suspend fun get(profileId: Long): UserWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: UserWallet)
}
