package com.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jurassicjournal.data.game.entity.Dino
import kotlinx.coroutines.flow.Flow

@Dao
interface DinoDao {

    @Query("SELECT COUNT(*) FROM dinos")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dinos: List<Dino>)

    @Query("""
        SELECT * FROM dinos
        WHERE (:nameQuery = '' OR LOWER(name) LIKE '%' || LOWER(:nameQuery) || '%')
        AND (:rarity = '' OR rarity = :rarity)
        AND (:dinoClass = '' OR dinoClass = :dinoClass)
        ORDER BY name ASC
    """)
    fun observeDinos(nameQuery: String, rarity: String, dinoClass: String): Flow<List<Dino>>

    @Query("SELECT * FROM dinos WHERE id = :id")
    suspend fun getById(id: Long): Dino?

    @Query("SELECT id, slug FROM dinos")
    suspend fun getAllSlugIds(): List<DinoSlugId>

    @Query("SELECT id, name FROM dinos")
    suspend fun getAllNameIds(): List<DinoNameId>
}

data class DinoSlugId(val id: Long, val slug: String)
data class DinoNameId(val id: Long, val name: String)
