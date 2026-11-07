package com.sufficienteffort.jurassicjournal.data.game.repository

import com.sufficienteffort.jurassicjournal.data.game.dao.DinoDao
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoMoveDao
import com.sufficienteffort.jurassicjournal.data.game.dao.MoveDao
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.game.entity.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the ability detail screen shows for one ability. */
data class AbilityDetail(
    val move: Move,
    val normal: MoveVariant,
    val threatened: MoveVariant?,
    val dinos: List<Dino>,
)

@Singleton
class AbilityRepository @Inject constructor(
    private val moveDao: MoveDao,
    private val dinoMoveDao: DinoMoveDao,
    private val dinoDao: DinoDao,
) {
    /** Every ability as list entries; abilities with a threatened variant yield two entries. */
    fun observeEntries(): Flow<List<AbilityEntry>> =
        moveDao.observeAll()
            .map { moves ->
                moves.flatMap { move ->
                    val (normal, threatened) = move.variants()
                    AbilityEntries.fromMove(move, normal, threatened)
                }
            }
            .flowOn(Dispatchers.Default)

    /** dinoId → slugs of the abilities that dino has. */
    fun observeSlugsByDino(): Flow<Map<Long, Set<String>>> =
        dinoMoveDao.observeLinks()
            .map { links -> links.groupBy({ it.dinoId }, { it.moveSlug }).mapValues { it.value.toSet() } }
            .flowOn(Dispatchers.Default)

    suspend fun getDetail(slug: String): AbilityDetail? {
        val move = moveDao.getBySlug(slug) ?: return null
        val (normal, threatened) = move.variants()
        val dinos = dinoDao.getByIds(dinoMoveDao.getDinoIdsForMove(slug)).sortedBy { it.name }
        return AbilityDetail(move, normal, threatened, dinos)
    }
}
