package com.jurassicjournal.data.user.entity

import androidx.room.Entity

@Entity(
    tableName = "user_boosts",
    primaryKeys = ["profileId", "dinoId", "stat"],
)
data class UserBoost(
    val profileId: Long = 1,
    val dinoId: Long,
    val stat: String,
    val boostsApplied: Int = 0,
)
