package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.Move

@Dao
interface MoveDao {
    @Query("SELECT * FROM moves WHERE slug IN (:slugs)")
    suspend fun getBySlugList(slugs: List<String>): List<Move>

    @Query("SELECT mainIconPath, overlayIconsJson FROM moves")
    suspend fun getAllIconData(): List<MoveIconData>
}

data class MoveIconData(val mainIconPath: String?, val overlayIconsJson: String?)
