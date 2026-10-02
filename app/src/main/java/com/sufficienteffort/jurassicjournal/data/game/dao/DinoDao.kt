package com.sufficienteffort.jurassicjournal.data.game.dao

import androidx.room.Dao
import androidx.room.Query
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.HybridType
import com.sufficienteffort.jurassicjournal.data.model.ProgressionSystem
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import kotlinx.coroutines.flow.Flow

@Dao
interface DinoDao {

    /** Every dino joined with each of its move names; one row per (dino, move). Filtering happens in Kotlin. */
    @Query("""
        SELECT d.*, m.name AS matchedMoveName
        FROM dinos d
        LEFT JOIN dino_moves dm ON dm.dinoId = d.id
        LEFT JOIN moves m ON m.slug = dm.moveSlug
        ORDER BY d.name ASC, m.name ASC
    """)
    fun observeDinoMovePairs(): Flow<List<DinoMoveRow>>

    @Query("SELECT * FROM dinos WHERE id = :id")
    suspend fun getById(id: Long): Dino?

    @Query("SELECT * FROM dinos WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Dino>

    @Query("SELECT id, slug FROM dinos")
    suspend fun getAllSlugIds(): List<DinoSlugId>

    @Query("SELECT imagePath FROM dinos")
    suspend fun getAllImagePaths(): List<String>
}

data class DinoMoveRow(
    val id: Long,
    val slug: String,
    val name: String,
    val description: String,
    val rarity: Rarity,
    val dinoClass: DinoClass,
    val hybridType: HybridType,
    val imagePath: String,
    val isHybrid: Boolean,
    val sanctuaryEligible: Boolean,
    val progressionSystem: ProgressionSystem,
    val matchedMoveName: String?,
) {
    fun toDino(): Dino = Dino(
        id = id, slug = slug, name = name, description = description,
        rarity = rarity, dinoClass = dinoClass, hybridType = hybridType,
        imagePath = imagePath, isHybrid = isHybrid,
        sanctuaryEligible = sanctuaryEligible, progressionSystem = progressionSystem,
    )
}

data class DinoSlugId(val id: Long, val slug: String)
