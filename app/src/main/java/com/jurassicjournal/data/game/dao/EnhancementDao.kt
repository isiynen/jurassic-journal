package com.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.jurassicjournal.data.game.entity.Enhancement
import com.jurassicjournal.data.game.entity.EnhancementStatBonus

@Dao
interface EnhancementDao {
    @Query("SELECT * FROM enhancements WHERE dinoId = :dinoId ORDER BY enhancementTier ASC")
    suspend fun getForDino(dinoId: Long): List<Enhancement>

    @Query("SELECT * FROM enhancement_stat_bonuses WHERE enhancementId IN (:ids)")
    suspend fun getStatBonuses(ids: List<Long>): List<EnhancementStatBonus>
}
