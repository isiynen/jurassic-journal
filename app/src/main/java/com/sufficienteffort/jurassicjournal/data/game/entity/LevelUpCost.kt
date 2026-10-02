package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import com.sufficienteffort.jurassicjournal.data.model.Rarity

@Entity(
    tableName = "level_up_costs",
    primaryKeys = ["rarity", "fromLevel"]
)
data class LevelUpCost(
    val rarity: Rarity,
    val fromLevel: Int,
    val toLevel: Int,
    val coinsCost: Int,
    val dnaCost: Int
)
