package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoSanctuaryPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface DinoSanctuaryPointDao {
    @Query("SELECT * FROM dino_sanctuary_points WHERE dinoId = :dinoId")
    suspend fun getForDino(dinoId: Long): DinoSanctuaryPoint?

    @Query("SELECT * FROM dino_sanctuary_points")
    fun observeAll(): Flow<List<DinoSanctuaryPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<DinoSanctuaryPoint>)
}
