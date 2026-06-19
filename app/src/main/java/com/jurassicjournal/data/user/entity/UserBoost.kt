package com.jurassicjournal.data.user.entity

import androidx.room.Entity

@Entity(
    tableName = "user_boosts",
    primaryKeys = ["dinoId", "stat"]
)
data class UserBoost(
    val dinoId: Long,
    val stat: String,
    val boostsApplied: Int = 0
)
