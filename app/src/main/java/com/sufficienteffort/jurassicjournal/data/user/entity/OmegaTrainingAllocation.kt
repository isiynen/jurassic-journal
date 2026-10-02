package com.sufficienteffort.jurassicjournal.data.user.entity

import androidx.room.Entity

@Entity(
    tableName = "omega_training_allocations",
    primaryKeys = ["profileId", "dinoId", "stat"],
)
data class OmegaTrainingAllocation(
    val profileId: Long = 1,
    val dinoId: Long,
    val stat: String,
    val pointsAllocated: Int = 0,
)
