package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoResistance
import kotlinx.coroutines.flow.Flow

@Dao
interface DinoResistanceDao {
    @Query("SELECT * FROM dino_resistances WHERE dinoId = :dinoId")
    suspend fun getForDino(dinoId: Long): List<DinoResistance>

    @Query("SELECT * FROM dino_resistances")
    fun observeAll(): Flow<List<DinoResistance>>
}
