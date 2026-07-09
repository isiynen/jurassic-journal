package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sufficienteffort.jurassicjournal.data.model.EffectTarget

@Entity(tableName = "move_effects")
data class MoveEffect(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moveId: Long,
    val effectType: String,
    val target: EffectTarget,
    val value: Float,
    val duration: Int = 0,
    val isPercentage: Boolean = false
)
