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
        dinoDao.observeDinoMovePairs(
            rarity = rarity?.name ?: "",
            dinoClass = dinoClass?.name ?: "",
        ).map { rows ->
            val all = rows.groupIntoResults()
            when {
                query.isBlank() -> all.map { it.copy(matchedMoves = emptyList()) }
                isStrictQuery(query) -> filterStrict(query.drop(1).dropLast(1), all)
                else -> filterMultiWord(query, all)
            }
        }

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

// ── Query parsing ─────────────────────────────────────────────────────────────

private fun isStrictQuery(query: String): Boolean =
    (query.startsWith('"') && query.endsWith('"') && query.length >= 2) ||
    (query.startsWith('\'') && query.endsWith('\'') && query.length >= 2)

// ── Strict mode: single phrase, exact substring ───────────────────────────────

private fun filterStrict(phrase: String, all: List<DinoSearchResult>): List<DinoSearchResult> {
    val p = phrase.lowercase()
    return all.mapNotNull { result ->
        val nameHits = result.dino.name.lowercase().contains(p)
        val moveHits = result.matchedMoves.filter { it.lowercase().contains(p) }
        if (!nameHits && moveHits.isEmpty()) null
        else result.copy(matchedMoves = if (nameHits) emptyList() else moveHits)
    }
}

// ── Multi-word mode: all words must appear somewhere ─────────────────────────
//
// Rules:
//   • Split query on whitespace into individual words.
//   • A dino is included when, for every word, either:
//       – the dino's name contains that word, OR
//       – at least one of the dino's move names contains that word.
//   • The "matched moves" shown on the card are those moves that contain at
//     least one word that isn't already covered by the dino's own name
//     (i.e. the moves that explain why a name-only search wouldn't find it).

private fun filterMultiWord(query: String, all: List<DinoSearchResult>): List<DinoSearchResult> {
    val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return all.map { it.copy(matchedMoves = emptyList()) }

    return all.mapNotNull { result ->
        val nameLower = result.dino.name.lowercase()
        val movesLower = result.matchedMoves.map { it.lowercase() }

        val allMatch = words.all { w ->
            nameLower.contains(w) || movesLower.any { it.contains(w) }
        }
        if (!allMatch) return@mapNotNull null

        // Only highlight moves that contribute a word not already in the dino name
        val wordsNotInName = words.filter { !nameLower.contains(it) }
        val relevant = if (wordsNotInName.isEmpty()) {
            emptyList()
        } else {
            result.matchedMoves.filter { move ->
                val ml = move.lowercase()
                wordsNotInName.any { w -> ml.contains(w) }
            }
        }

        result.copy(matchedMoves = relevant)
    }
}

// ── Row grouping ──────────────────────────────────────────────────────────────

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
