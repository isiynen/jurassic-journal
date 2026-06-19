package com.jurassicjournal.data.game.entity

import androidx.room.Entity
import com.jurassicjournal.data.model.ResistanceType

@Entity(
    tableName = "dino_resistances",
    primaryKeys = ["dinoId", "resistType"]
)
data class DinoResistance(
    val dinoId: Long,
    val resistType: ResistanceType,
    val percentage: Int
)
