package com.sufficienteffort.jurassicjournal.data.user.entity

import androidx.room.Entity

@Entity(
    tableName = "user_wallet",
    primaryKeys = ["profileId"],
)
data class UserWallet(
    val profileId: Long = 1,
    val coins: Long = 0,
    val hardCash: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
)
