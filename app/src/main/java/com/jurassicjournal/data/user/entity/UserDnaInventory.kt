package com.jurassicjournal.data.user.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_dna_inventory")
data class UserDnaInventory(
    @PrimaryKey val dinoId: Long,
    val dnaAmount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
