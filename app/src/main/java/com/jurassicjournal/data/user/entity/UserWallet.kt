package com.jurassicjournal.data.user.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_wallet")
data class UserWallet(
    @PrimaryKey val id: Int = 1,
    val coins: Long = 0,
    val hardCash: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
