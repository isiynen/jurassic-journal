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

    @Query("""
        SELECT d.*, m.name AS matchedMoveName
        FROM dinos d
        LEFT JOIN dino_moves dm ON dm.dinoId = d.id
        LEFT JOIN moves m ON m.slug = dm.moveSlug
        WHERE (:rarity = '' OR d.rarity = :rarity)
        AND (:dinoClass = '' OR d.dinoClass = :dinoClass)
        ORDER BY d.name ASC, m.name ASC
    """)
    fun observeDinoMovePairs(rarity: String, dinoClass: String): Flow<List<DinoMoveRow>>

    @Query("SELECT * FROM dinos WHERE id = :id")
    suspend fun getById(id: Long): Dino?

    @Query("SELECT * FROM dinos WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Dino>

    @Query("SELECT id, slug FROM dinos")
    suspend fun getAllSlugIds(): List<DinoSlugId>

    @Query("SELECT id, name FROM dinos")
    suspend fun getAllNameIds(): List<DinoNameId>

    @Query("SELECT imagePath FROM dinos")
    suspend fun getAllImagePaths(): List<String>
}

data class DinoMoveRow(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String,
    val rarity: com.jurassicjournal.data.model.Rarity,
    val dinoClass: com.jurassicjournal.data.model.DinoClass,
    val hybridType: com.jurassicjournal.data.model.HybridType,
    val imagePath: String,
    val isHybrid: Boolean,
    val sanctuaryEligible: Boolean,
    val progressionSystem: com.jurassicjournal.data.model.ProgressionSystem,
    val matchedMoveName: String?,
)

data class DinoSlugId(val id: Long, val slug: String)
data class DinoNameId(val id: Long, val name: String)
