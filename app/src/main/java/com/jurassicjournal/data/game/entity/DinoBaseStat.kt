package com.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dino_base_stats")
data class DinoBaseStat(
    @PrimaryKey val dinoId: Long,
    val baseHealth: Int,
    val baseAttack: Int,
    val speed: Int,
    val armor: Float,
    val critChance: Float,
    val critMultiplier: Float = 1.25f
)
