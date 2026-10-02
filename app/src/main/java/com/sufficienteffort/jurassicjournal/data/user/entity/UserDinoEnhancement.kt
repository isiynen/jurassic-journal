package com.sufficienteffort.jurassicjournal.data.user.entity

import androidx.room.Entity

@Entity(
    tableName = "user_dino_enhancements",
    primaryKeys = ["profileId", "dinoId", "enhancementId"],
)
data class UserDinoEnhancement(
    val profileId: Long = 1,
    val dinoId: Long,
    val enhancementId: Long,
    val isUnlocked: Boolean = false,
    val unlockedDate: Long? = null,
)
