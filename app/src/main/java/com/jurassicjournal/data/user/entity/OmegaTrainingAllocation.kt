package com.jurassicjournal.data.user.entity

import androidx.room.Entity

@Entity(
    tableName = "omega_training_allocations",
    primaryKeys = ["dinoId", "stat"]
)
data class OmegaTrainingAllocation(
    val dinoId: Long,
    val stat: String,
    val pointsAllocated: Int = 0
)
