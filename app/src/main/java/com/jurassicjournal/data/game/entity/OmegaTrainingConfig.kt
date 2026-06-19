package com.jurassicjournal.data.game.entity

import androidx.room.Entity

@Entity(tableName = "omega_training_configs", primaryKeys = ["dinoId", "stat"])
data class OmegaTrainingConfig(
    val dinoId: Long,
    val stat: String,       // "health" | "attack" | "speed" | "armor" | "crit_chance" | "crit_multiplier"
    val gainPerPoint: Int,  // stat increase per training point
    val pointCap: Int,      // max points allocatable to this stat
    val maxCap: Int,        // absolute stat ceiling (base + pointCap * gainPerPoint)
)
