package com.sufficienteffort.jurassicjournal.data.user.entity

import androidx.room.Entity

@Entity(
    tableName = "user_dinos",
    primaryKeys = ["profileId", "dinoId"],
)
data class UserDino(
    val profileId: Long = 1,
    val dinoId: Long,
    val isUnlocked: Boolean = false,
    val currentLevel: Int = 1,
    val currentXp: Int = 0,
)
