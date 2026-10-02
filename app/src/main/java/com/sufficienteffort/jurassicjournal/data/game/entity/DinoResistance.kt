package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import com.sufficienteffort.jurassicjournal.data.model.ResistanceType

@Entity(
    tableName = "dino_resistances",
    primaryKeys = ["dinoId", "resistType"]
)
data class DinoResistance(
    val dinoId: Long,
    val resistType: ResistanceType,
    val percentage: Int
)
