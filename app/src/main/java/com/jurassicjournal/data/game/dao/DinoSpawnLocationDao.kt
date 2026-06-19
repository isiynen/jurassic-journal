package com.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.game.entity.DinoSpawnLocation

@Dao
interface DinoSpawnLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<DinoSpawnLocation>)

    @Query("SELECT * FROM dino_spawn_locations WHERE dinoId = :dinoId")
    suspend fun getForDino(dinoId: Long): List<DinoSpawnLocation>
}
