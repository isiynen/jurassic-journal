package com.sufficienteffort.jurassicjournal.data.game.repository

import com.sufficienteffort.jurassicjournal.data.game.dao.DinoDao
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoMoveRow
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoSpawnLocationDao
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.SpawnLocation
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.NewDinoDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class DinoSearchResult(
    val dino: Dino,
    val matchedMoves: List<String>,
    val isNew: Boolean = false,
)

@Singleton
class DinoRepository @Inject constructor(
    private val dinoDao: DinoDao,
    private val newDinoDao: NewDinoDao,
    private val activeProfileRepository: ActiveProfileRepository,
    private val dinoSpawnLocationDao: DinoSpawnLocationDao,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun search(
        query: String = "",
        rarities: Set<Rarity> = emptySet(),
        dinoClasses: Set<DinoClass> = emptySet(),
        locations: Set<SpawnLocation> = emptySet(),
    ): Flow<List<DinoSearchResult>> =
        activeProfileRepository.activeProfileId.flatMapLatest { profileId ->
            combine(
                dinoDao.observeDinoMovePairs(
                    rarity = "",
                    dinoClass = "",
                ),
                newDinoDao.observeNewSlugs(profileId),
                dinoSpawnLocationDao.observeAll(),
            ) { rows, newSlugs, allSpawnLocs ->
                val spawnMap: Map<Long, Set<SpawnLocation>> = allSpawnLocs
                    .groupBy({ it.dinoId }, { it.location })
                    .mapValues { (_, locs) -> locs.toSet() }
                val newSlugSet = newSlugs.toSet()
                val all = rows.groupIntoResults(newSlugSet)
                val rarityFiltered = if (rarities.isEmpty()) all
                else all.filter { it.dino.rarity in rarities }
                val classFiltered = if (dinoClasses.isEmpty()) rarityFiltered
                else rarityFiltered.filter { it.dino.dinoClass in dinoClasses }
                val locationFiltered = if (locations.isEmpty()) classFiltered
                else classFiltered.filter { result ->
                    val dinoLocs = spawnMap[result.dino.id] ?: emptySet()
                    locations.all { it in dinoLocs }
                }
                val filtered = when {
                    query.isBlank() -> locationFiltered.map { it.copy(matchedMoves = emptyList()) }
                    isStrictQuery(query) -> filterStrict(query.drop(1).dropLast(1), locationFiltered)
                    else -> filterMultiWord(query, locationFiltered)
                }
                // New dinos float to top (alphabetical), rest follow in their existing order
                val (newOnes, rest) = filtered.partition { it.isNew }
                newOnes.sortedBy { it.dino.name } + rest
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeNewCount(): Flow<Int> =
        activeProfileRepository.activeProfileId.flatMapLatest { profileId ->
            newDinoDao.observeNewCount(profileId)
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

private fun List<DinoMoveRow>.groupIntoResults(newSlugs: Set<String> = emptySet()): List<DinoSearchResult> {
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
            isNew = row.slug in newSlugs,
        )
    }
    return seen.values.toList()
}
