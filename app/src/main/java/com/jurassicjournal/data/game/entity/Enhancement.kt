package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sufficienteffort.jurassicjournal.data.model.EnhancementType

@Entity(tableName = "enhancements")
data class Enhancement(
    @PrimaryKey val id: Long = 0,
    val dinoId: Long,
    val enhancementTier: Int,
    val name: String,
    val description: String,
    val enhancementType: EnhancementType
)
