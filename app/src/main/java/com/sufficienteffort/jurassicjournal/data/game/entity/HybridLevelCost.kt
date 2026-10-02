package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity

@Entity(
    tableName = "hybrid_level_costs",
    primaryKeys = ["hybridDinoId", "fromLevel", "ingredientDinoId"]
)
data class HybridLevelCost(
    val hybridDinoId: Long,
    val fromLevel: Int,
    val toLevel: Int,
    val ingredientDinoId: Long,
    val dnaAmount: Int
)
