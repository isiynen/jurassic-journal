package com.jurassicjournal.data.user.entity

import androidx.room.Entity

@Entity(
    tableName = "user_dino_enhancements",
    primaryKeys = ["dinoId", "enhancementId"]
)
data class UserDinoEnhancement(
    val dinoId: Long,
    val enhancementId: Long,
    val isUnlocked: Boolean = false,
    val unlockedDate: Long? = null
)
