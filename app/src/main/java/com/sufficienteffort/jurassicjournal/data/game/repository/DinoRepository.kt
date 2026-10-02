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
import javax.inject.Inject
import javax.inject.Singleton

data class DinoSearchResult(
    val dino: Dino,
    val matchedMoves: List<String>,
    val isNew: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DinoRepository @Inject constructor(
    private val dinoDao: DinoDao,
    private val newDinoDao: NewDinoDao,
    private val activeProfileRepository: ActiveProfileRepository,
    private val dinoSpawnLocationDao: DinoSpawnLocationDao,
) {

    fun search(
        query: String = "",
        rarities: Set<Rarity> = emptySet(),
        dinoClasses: Set<DinoClass> = emptySet(),
        locations: Set<SpawnLocation> = emptySet(),
    ): Flow<List<DinoSearchResult>> =
        activeProfileRepository.activeProfileId.flatMapLatest { profileId ->
            combine(
                dinoDao.observeDinoMovePairs(),
                newDinoDao.observeNewSlugs(profileId),
                dinoSpawnLocationDao.observeAll(),
            ) { rows, newSlugs, allSpawnLocs ->
                val spawnMap: Map<Long, Set<SpawnLocation>> = allSpawnLocs
                    .groupBy({ it.dinoId }, { it.location })
                    .mapValues { (_, locs) -> locs.toSet() }
                DinoSearchFilter.apply(
                    all = rows.groupIntoResults(newSlugs.toSet()),
                    query = query,
                    rarities = rarities,
                    dinoClasses = dinoClasses,
                    locations = locations,
                    spawnMap = spawnMap,
                )
            }
        }

    fun observeNewCount(): Flow<Int> =
        activeProfileRepository.activeProfileId.flatMapLatest { profileId ->
            newDinoDao.observeNewCount(profileId)
        }

    suspend fun getDinosByIds(ids: List<Long>): List<Dino> = dinoDao.getByIds(ids)
}

// ── Row grouping ──────────────────────────────────────────────────────────────

/** Collapses one-row-per-move join results into one [DinoSearchResult] per dino. */
internal fun List<DinoMoveRow>.groupIntoResults(newSlugs: Set<String> = emptySet()): List<DinoSearchResult> {
    val seen = LinkedHashMap<Long, DinoSearchResult>()
    for (row in this) {
        val existing = seen[row.id]
        val moves = if (row.matchedMoveName != null)
            (existing?.matchedMoves ?: emptyList()) + row.matchedMoveName
        else
            existing?.matchedMoves ?: emptyList()
        seen[row.id] = existing?.copy(matchedMoves = moves)
            ?: DinoSearchResult(dino = row.toDino(), matchedMoves = moves, isNew = row.slug in newSlugs)
    }
    return seen.values.toList()
}

// ── Filtering / query parsing ─────────────────────────────────────────────────

/**
 * Pure filtering over the full dino list. Split out of the repository so it can
 * be unit tested without Room.
 *
 * Query rules:
 *   • Quoted ("…" or '…') → strict mode: the phrase must appear as an exact
 *     substring of the dino name or one of its move names.
 *   • Otherwise → multi-word mode: split on whitespace; every word must appear in
 *     the name or in at least one move name. The "matched moves" shown on the card
 *     are those containing a word the name itself doesn't cover.
 */
internal object DinoSearchFilter {

    fun apply(
        all: List<DinoSearchResult>,
        query: String,
        rarities: Set<Rarity>,
        dinoClasses: Set<DinoClass>,
        locations: Set<SpawnLocation>,
        spawnMap: Map<Long, Set<SpawnLocation>>,
    ): List<DinoSearchResult> {
        var list = all
        if (rarities.isNotEmpty()) list = list.filter { it.dino.rarity in rarities }
        if (dinoClasses.isNotEmpty()) list = list.filter { it.dino.dinoClass in dinoClasses }
        if (locations.isNotEmpty()) list = list.filter { result ->
            val dinoLocs = spawnMap[result.dino.id] ?: emptySet()
            locations.all { it in dinoLocs }
        }
        val filtered = when {
            query.isBlank()      -> list.map { it.copy(matchedMoves = emptyList()) }
            isStrictQuery(query) -> filterStrict(query.drop(1).dropLast(1), list)
            else                 -> filterMultiWord(query, list)
        }
        // New dinos float to top (alphabetical), rest follow in their existing order
        val (newOnes, rest) = filtered.partition { it.isNew }
        return newOnes.sortedBy { it.dino.name } + rest
    }

    fun isStrictQuery(query: String): Boolean =
        query.length >= 2 && (
            (query.startsWith('"') && query.endsWith('"')) ||
            (query.startsWith('\'') && query.endsWith('\''))
        )

    fun filterStrict(phrase: String, all: List<DinoSearchResult>): List<DinoSearchResult> {
        val p = phrase.lowercase()
        return all.mapNotNull { result ->
            val nameHits = result.dino.name.lowercase().contains(p)
            val moveHits = result.matchedMoves.filter { it.lowercase().contains(p) }
            if (!nameHits && moveHits.isEmpty()) null
            else result.copy(matchedMoves = if (nameHits) emptyList() else moveHits)
        }
    }

    fun filterMultiWord(query: String, all: List<DinoSearchResult>): List<DinoSearchResult> {
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
}
