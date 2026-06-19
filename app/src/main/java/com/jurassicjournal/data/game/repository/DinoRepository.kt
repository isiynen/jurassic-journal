package com.jurassicjournal.data.game.repository

import com.jurassicjournal.data.game.dao.DinoDao
import com.jurassicjournal.data.game.dao.DinoMoveRow
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.model.DinoClass
import com.jurassicjournal.data.model.Rarity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class DinoSearchResult(
    val dino: Dino,
    val matchedMoves: List<String>,
)

@Singleton
class DinoRepository @Inject constructor(private val dinoDao: DinoDao) {

    fun search(
        query: String = "",
        rarity: Rarity? = null,
        dinoClass: DinoClass? = null,
    ): Flow<List<DinoSearchResult>> =
        dinoDao.observeDinoMoveSearch(
            query = query,
            rarity = rarity?.name ?: "",
            dinoClass = dinoClass?.name ?: "",
        ).map { rows -> rows.groupIntoResults() }

    suspend fun getDinosByIds(ids: List<Long>): List<Dino> = dinoDao.getByIds(ids)

    fun getDinos(
        nameQuery: String = "",
        rarity: Rarity? = null,
        dinoClass: DinoClass? = null,
    ): Flow<List<Dino>> = dinoDao.observeDinos(
        nameQuery = nameQuery,
        rarity = rarity?.name ?: "",
        dinoClass = dinoClass?.name ?: "",
    )
}

private fun List<DinoMoveRow>.groupIntoResults(): List<DinoSearchResult> {
    val seen = LinkedHashMap<Long, DinoSearchResult>()
    for (row in this) {
        val existing = seen[row.id]
        val moves = if (row.matchedMoveName != null)
            (existing?.matchedMoves ?: emptyList()) + row.matchedMoveName
        else
            existing?.matchedMoves ?: emptyList()
        seen[row.id] = DinoSearchResult(
            dino = Dino(
                id = row.id, slug = row.slug, name = row.name,
                description = row.description, rarity = row.rarity,
                dinoClass = row.dinoClass, hybridType = row.hybridType,
                imagePath = row.imagePath, isHybrid = row.isHybrid,
                sanctuaryEligible = row.sanctuaryEligible,
                progressionSystem = row.progressionSystem,
            ),
            matchedMoves = moves,
        )
    }
    return seen.values.toList()
}
