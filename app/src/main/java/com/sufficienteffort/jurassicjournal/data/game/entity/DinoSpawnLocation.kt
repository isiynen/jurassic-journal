package com.sufficienteffort.jurassicjournal.data.game.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sufficienteffort.jurassicjournal.data.model.SpawnLocation

@Entity(tableName = "dino_spawn_locations")
data class DinoSpawnLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dinoId: Long,
    val location: SpawnLocation,
    val timeOfDay: String? = null
)
