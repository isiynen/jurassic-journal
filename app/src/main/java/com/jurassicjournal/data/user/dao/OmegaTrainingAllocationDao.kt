package com.jurassicjournal.data.user.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.user.entity.OmegaTrainingAllocation

@Dao
interface OmegaTrainingAllocationDao {
    @Query("SELECT * FROM omega_training_allocations WHERE dinoId = :dinoId")
    suspend fun getForDino(dinoId: Long): List<OmegaTrainingAllocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(allocation: OmegaTrainingAllocation)
}
