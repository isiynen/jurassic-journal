package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import com.sufficienteffort.jurassicjournal.data.model.MoveTriggerType
import com.sufficienteffort.jurassicjournal.data.model.MoveUnlockType

@Entity(
    tableName = "dino_moves",
    primaryKeys = ["dinoId", "moveSlug", "triggerType"]
)
data class DinoMove(
    val dinoId: Long,
    val moveSlug: String,
    val slotOrder: Int,
    val triggerType: MoveTriggerType,
    val unlockType: MoveUnlockType = MoveUnlockType.DEFAULT,
    val unlockValue: String? = null
)
