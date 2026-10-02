package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoBaseStat
import kotlinx.coroutines.flow.Flow

@Dao
interface DinoBaseStatDao {
    @Query("SELECT * FROM dino_base_stats WHERE dinoId = :dinoId")
    suspend fun getByDinoId(dinoId: Long): DinoBaseStat?

    @Query("SELECT * FROM dino_base_stats")
    fun observeAll(): Flow<List<DinoBaseStat>>
}
