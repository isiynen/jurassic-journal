package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoMove

@Dao
interface DinoMoveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dinoMoves: List<DinoMove>)

    @Query("SELECT * FROM dino_moves WHERE dinoId = :dinoId ORDER BY slotOrder ASC")
    suspend fun getForDino(dinoId: Long): List<DinoMove>
}
