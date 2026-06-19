package com.jurassicjournal.data.game.entity

import androidx.room.Entity
import com.jurassicjournal.data.model.CatalystType

@Entity(
    tableName = "enhancement_catalyst_costs",
    primaryKeys = ["enhancementId", "catalystType"]
)
data class EnhancementCatalystCost(
    val enhancementId: Long,
    val catalystType: CatalystType,
    val quantityRequired: Int
)
