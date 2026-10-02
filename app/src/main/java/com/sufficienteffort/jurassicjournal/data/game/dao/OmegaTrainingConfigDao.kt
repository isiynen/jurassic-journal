package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.OmegaTrainingConfig

@Dao
interface OmegaTrainingConfigDao {
    @Query("SELECT * FROM omega_training_configs WHERE dinoId = :dinoId")
    suspend fun getForDino(dinoId: Long): List<OmegaTrainingConfig>
}
