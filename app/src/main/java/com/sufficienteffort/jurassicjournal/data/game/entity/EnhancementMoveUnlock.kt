package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sufficienteffort.jurassicjournal.data.model.MoveTriggerType

@Entity(tableName = "enhancement_move_unlocks")
data class EnhancementMoveUnlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val enhancementId: Long,
    val moveSlug: String,
    val triggerType: MoveTriggerType
)
