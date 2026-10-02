package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dino_sanctuary_points",
    indices = [Index("dinoId")],
)
data class DinoSanctuaryPoint(
    @PrimaryKey val dinoId: Long,
    val spMaxBoost: Int,
    val spBaseline: Int,
    val spSad: Double,
)
