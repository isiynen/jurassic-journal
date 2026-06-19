package com.jurassicjournal.data.user.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_dinos")
data class UserDino(
    @PrimaryKey val dinoId: Long,
    val isUnlocked: Boolean = false,
    val currentLevel: Int = 1,
    val currentXp: Int = 0
)
