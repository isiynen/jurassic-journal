package com.jurassicjournal.data.user.entity

import androidx.room.Entity

@Entity(
    tableName = "user_dna_inventory",
    primaryKeys = ["profileId", "dinoId"],
)
data class UserDnaInventory(
    val profileId: Long = 1,
    val dinoId: Long,
    val dnaAmount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
)
