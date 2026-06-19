package com.jurassicjournal.data.game.repository

import com.jurassicjournal.data.game.dao.DinoBaseStatDao
import com.jurassicjournal.data.game.dao.DinoDao
import com.jurassicjournal.data.game.dao.DinoHybridIngredientDao
import com.jurassicjournal.data.game.dao.DinoMoveDao
import com.jurassicjournal.data.game.dao.DinoResistanceDao
import com.jurassicjournal.data.game.dao.DinoSanctuaryPointDao
import com.jurassicjournal.data.game.dao.MoveDao
import com.jurassicjournal.data.game.dao.OmegaTrainingConfigDao
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.game.entity.DinoBaseStat
import com.jurassicjournal.data.game.entity.DinoMove
import com.jurassicjournal.data.game.entity.DinoResistance
import com.jurassicjournal.data.game.entity.DinoSanctuaryPoint
import com.jurassicjournal.data.game.entity.Move
import com.jurassicjournal.data.game.entity.OmegaTrainingConfig
import com.jurassicjournal.data.model.MovePriorityType
import com.jurassicjournal.data.model.MoveTriggerType
import com.jurassicjournal.data.model.ProgressionSystem
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTarget(val target: String, val effects: List<String>)

data class MoveVariant(
    val priority: MovePriorityType,
    val cooldown: Int,
    val delay: Int,
    val targets: List<ParsedTarget>,
)

data class DinoMoveDetail(
    val dinoMove: DinoMove,
    val move: Move,
    val secure: MoveVariant,
    val threatened: MoveVariant?,
)

data class IngredientNode(
    val dino: Dino,
    val children: List<IngredientNode> = emptyList(),
)

data class DinoFullDetail(
    val dino: Dino,
    val stats: DinoBaseStat,
    val resistances: List<DinoResistance>,
    val movesByTrigger: Map<MoveTriggerType, List<DinoMoveDetail>>,
    val omegaTrainingConfigs: List<OmegaTrainingConfig> = emptyList(),
    val ingredientTree: List<IngredientNode> = emptyList(),
    val sanctuaryPoints: DinoSanctuaryPoint? = null,
    val hybridsUsing: List<Dino> = emptyList(),
)

@Singleton
class DinoDetailRepository @Inject constructor(
    private val dinoDao: DinoDao,
    private val dinoBaseStatDao: DinoBaseStatDao,
    private val dinoResistanceDao: DinoResistanceDao,
    private val dinoMoveDao: DinoMoveDao,
    private val moveDao: MoveDao,
    private val omegaTrainingConfigDao: OmegaTrainingConfigDao,
    private val hybridIngredientDao: DinoHybridIngredientDao,
    private val dinoSanctuaryPointDao: DinoSanctuaryPointDao,
) {
    suspend fun getFullDetail(dinoId: Long): DinoFullDetail? {
        val dino = dinoDao.getById(dinoId) ?: return null
        val stats = dinoBaseStatDao.getByDinoId(dinoId) ?: return null
        val resistances = dinoResistanceDao.getForDino(dinoId)
            .filter { it.percentage > 0 }
            .sortedByDescending { it.percentage }
        val dinoMoves = dinoMoveDao.getForDino(dinoId)
        val moveMap = moveDao.getBySlugList(dinoMoves.map { it.moveSlug })
            .associateBy { it.slug }

        val moveDetails = dinoMoves.mapNotNull { dm ->
            val move = moveMap[dm.moveSlug] ?: return@mapNotNull null
            DinoMoveDetail(
                dinoMove = dm,
                move = move,
                secure = parseVariant(
                    priority = move.priority,
                    cooldown = move.cooldown,
                    delay = move.delay,
                    targetsJson = move.normalTargetsJson,
                ),
                threatened = if (move.threatenedTargetsJson != null) parseVariant(
                    priority = move.threatenedPriority ?: move.priority,
                    cooldown = move.threatenedCooldown ?: move.cooldown,
                    delay = move.threatenedDelay ?: move.delay,
                    targetsJson = move.threatenedTargetsJson,
                ) else null,
            )
        }

        val movesByTrigger = moveDetails.groupBy { it.dinoMove.triggerType }
            .toSortedMap(compareBy { TRIGGER_ORDER.indexOf(it) })

        val omegaConfigs = if (dino.progressionSystem == ProgressionSystem.TRAINING_POINT)
            omegaTrainingConfigDao.getForDino(dinoId)
        else
            emptyList()

        val ingredientTree = if (dino.isHybrid)
            buildIngredientTree(dinoId, mutableSetOf(dinoId))
        else
            emptyList()

        val sanctuaryPoints = dinoSanctuaryPointDao.getForDino(dinoId)

        val hybridsUsing = hybridIngredientDao.getHybridIdsForIngredient(dinoId)
            .mapNotNull { hybridId -> dinoDao.getById(hybridId) }
            .sortedBy { it.name }

        return DinoFullDetail(dino, stats, resistances, movesByTrigger, omegaConfigs, ingredientTree, sanctuaryPoints, hybridsUsing)
    }

    private suspend fun buildIngredientTree(
        hybridId: Long,
        visited: MutableSet<Long>,
    ): List<IngredientNode> {
        val ingredientIds = hybridIngredientDao.getIngredientIds(hybridId)
        return ingredientIds.mapNotNull { ingredientId ->
            if (ingredientId in visited) return@mapNotNull null
            visited.add(ingredientId)
            val ingredientDino = dinoDao.getById(ingredientId) ?: return@mapNotNull null
            val children = if (ingredientDino.isHybrid)
                buildIngredientTree(ingredientId, visited)
            else
                emptyList()
            IngredientNode(ingredientDino, children)
        }
    }

    private fun parseVariant(
        priority: MovePriorityType,
        cooldown: Int,
        delay: Int,
        targetsJson: String,
    ): MoveVariant {
        val arr = JSONArray(targetsJson)
        val targets = (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val effectsArr = obj.getJSONArray("effects")
            ParsedTarget(
                target = obj.getString("target"),
                effects = (0 until effectsArr.length()).map { effectsArr.getString(it) },
            )
        }
        return MoveVariant(priority, cooldown, delay, targets)
    }

    companion object {
        private val TRIGGER_ORDER = listOf(
            MoveTriggerType.SELECTABLE,
            MoveTriggerType.ON_SWAP_IN,
            MoveTriggerType.ON_COUNTER,
            MoveTriggerType.ON_ESCAPE,
            MoveTriggerType.REACTIVE,
        )
    }
}
