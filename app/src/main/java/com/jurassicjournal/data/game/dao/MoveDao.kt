package com.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.game.entity.Move

@Dao
interface MoveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(moves: List<Move>)

    @Query("SELECT * FROM moves WHERE slug = :slug")
    suspend fun getBySlug(slug: String): Move?

    @Query("SELECT * FROM moves WHERE slug IN (:slugs)")
    suspend fun getBySlugList(slugs: List<String>): List<Move>
}
