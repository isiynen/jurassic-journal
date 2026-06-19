package com.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.game.entity.DinoResistance

@Dao
interface DinoResistanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resistances: List<DinoResistance>)

    @Query("SELECT * FROM dino_resistances WHERE dinoId = :dinoId")
    suspend fun getForDino(dinoId: Long): List<DinoResistance>
}
