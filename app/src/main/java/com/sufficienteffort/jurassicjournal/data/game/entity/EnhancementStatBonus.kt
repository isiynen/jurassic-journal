package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enhancement_stat_bonuses")
data class EnhancementStatBonus(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val enhancementId: Long,
    val stat: String,
    val value: Float,
    val isPercentage: Boolean
)
