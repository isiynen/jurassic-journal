package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoSpawnLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface DinoSpawnLocationDao {
    @Query("SELECT * FROM dino_spawn_locations WHERE dinoId = :dinoId")
    suspend fun getForDino(dinoId: Long): List<DinoSpawnLocation>

    @Query("SELECT * FROM dino_spawn_locations")
    fun observeAll(): Flow<List<DinoSpawnLocation>>
}
