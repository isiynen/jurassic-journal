package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoMove
import kotlinx.coroutines.flow.Flow

@Dao
interface DinoMoveDao {
    @Query("SELECT * FROM dino_moves WHERE dinoId = :dinoId ORDER BY slotOrder ASC")
    suspend fun getForDino(dinoId: Long): List<DinoMove>

    @Query("SELECT DISTINCT dinoId, moveSlug FROM dino_moves")
    fun observeLinks(): Flow<List<DinoMoveLink>>

    @Query("SELECT DISTINCT dinoId FROM dino_moves WHERE moveSlug = :slug")
    suspend fun getDinoIdsForMove(slug: String): List<Long>
}

data class DinoMoveLink(val dinoId: Long, val moveSlug: String)
