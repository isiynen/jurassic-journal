package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoMove

@Dao
interface DinoMoveDao {
    @Query("SELECT * FROM dino_moves WHERE dinoId = :dinoId ORDER BY slotOrder ASC")
    suspend fun getForDino(dinoId: Long): List<DinoMove>
}
