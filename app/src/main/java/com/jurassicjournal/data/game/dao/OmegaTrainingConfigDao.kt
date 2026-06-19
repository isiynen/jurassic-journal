package com.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.game.entity.OmegaTrainingConfig

@Dao
interface OmegaTrainingConfigDao {
    @Query("SELECT * FROM omega_training_configs WHERE dinoId = :dinoId")
    suspend fun getForDino(dinoId: Long): List<OmegaTrainingConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<OmegaTrainingConfig>)
}
