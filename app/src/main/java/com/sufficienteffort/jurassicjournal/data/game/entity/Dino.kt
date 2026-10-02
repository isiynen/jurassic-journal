package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.HybridType
import com.sufficienteffort.jurassicjournal.data.model.ProgressionSystem
import com.sufficienteffort.jurassicjournal.data.model.Rarity

@Entity(tableName = "dinos")
data class Dino(
    @PrimaryKey val id: Long = 0,
    val slug: String,
    val name: String,
    val description: String,
    val rarity: Rarity,
    val dinoClass: DinoClass,
    val hybridType: HybridType,
    val imagePath: String,
    val isHybrid: Boolean,
    val sanctuaryEligible: Boolean,
    val progressionSystem: ProgressionSystem = ProgressionSystem.BOOST
)
