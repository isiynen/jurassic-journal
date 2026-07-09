package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sufficienteffort.jurassicjournal.data.model.MovePriorityType
import com.sufficienteffort.jurassicjournal.data.model.MoveTriggerType

@Entity(tableName = "moves")
data class Move(
    @PrimaryKey val slug: String,
    val name: String,
    val trigger: MoveTriggerType,
    val mainIconPath: String? = null,
    val overlayIconsJson: String? = null,
    val priority: MovePriorityType = MovePriorityType.NORMAL,
    val cooldown: Int = 0,
    val delay: Int = 0,
    val normalTargetsJson: String,
    val threatenedPriority: MovePriorityType? = null,
    val threatenedCooldown: Int? = null,
    val threatenedDelay: Int? = null,
    val threatenedTargetsJson: String? = null
)
