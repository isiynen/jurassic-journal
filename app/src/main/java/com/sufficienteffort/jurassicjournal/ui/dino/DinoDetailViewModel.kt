package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sufficienteffort.jurassicjournal.data.game.dao.EnhancementDao
import com.sufficienteffort.jurassicjournal.data.game.entity.EnhancementStatBonus
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoDetailRepository
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoFullDetail
import com.sufficienteffort.jurassicjournal.data.model.BoostStat
import com.sufficienteffort.jurassicjournal.data.model.BoostState
import com.sufficienteffort.jurassicjournal.data.model.OmegaStat
import com.sufficienteffort.jurassicjournal.data.model.ProgressionSystem
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.defaultLevel
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.NewDinoDao
import com.sufficienteffort.jurassicjournal.data.user.dao.OmegaTrainingAllocationDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserBoostDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoEnhancementDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.sufficienteffort.jurassicjournal.data.user.entity.OmegaTrainingAllocation
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDino
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDinoEnhancement
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDnaInventory
import com.sufficienteffort.jurassicjournal.ui.navigation.Screen
import com.sufficienteffort.jurassicjournal.util.StatCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Enhancement stat-bonus key that raises the total boost cap. */
private const val BONUS_MAX_BOOSTS = "max_boosts"
private const val ENHANCEMENT_UNLOCK_LEVEL = 30

data class ComputedStats(
    val health: Int,
    val attack: Int,
    val speed: Int,
    val armor: Float,
    val critChance: Float,
    val critMultiplier: Float,
)

data class EnhancementUiItem(
    val id: Long,
    val tier: Int,
    val description: String,
    val isUnlocked: Boolean,
    val isAvailable: Boolean,
)

data class PendingEnhancementUncheck(
    val tier: Int,
    val cascadeTiers: List<Int>,
    val boostsTrimmed: Int,
)

data class DinoDetailUiState(
    val detail: DinoFullDetail? = null,
    val level: Int = 26,
    val boosts: BoostState = BoostState(),
    val omegaPoints: Map<String, Int> = emptyMap(),
    val computed: ComputedStats? = null,
    val hasUnsavedChanges: Boolean = false,
    val isLoading: Boolean = true,
    val dnaOnHand: Int = 0,
    val isNew: Boolean = false,
    val enhancementItems: List<EnhancementUiItem> = emptyList(),
    val maxTotalBoosts: Int = 0,
)

/** The user-editable trio; kept twice (current + last saved) to derive hasUnsavedChanges. */
private data class Inputs(
    val level: Int,
    val boosts: BoostState,
    val omegaPoints: Map<String, Int>,
)

private data class StoredEnhancement(
    val id: Long,
    val tier: Int,
    val description: String,
    val statBonuses: List<EnhancementStatBonus>,
    val isUnlocked: Boolean,
)

private fun List<StoredEnhancement>.boostCapBonus(): Int =
    filter { it.isUnlocked }
        .flatMap { it.statBonuses }
        .filter { it.stat == BONUS_MAX_BOOSTS && !it.isPercentage }
        .sumOf { it.value.toInt() }

@HiltViewModel
class DinoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val detailRepository: DinoDetailRepository,
    private val activeProfileRepository: ActiveProfileRepository,
    private val userDinoDao: UserDinoDao,
    private val userBoostDao: UserBoostDao,
    private val omegaAllocationDao: OmegaTrainingAllocationDao,
    private val userDnaInventoryDao: UserDnaInventoryDao,
    private val newDinoDao: NewDinoDao,
    private val enhancementDao: EnhancementDao,
    private val userEnhancementDao: UserDinoEnhancementDao,
) : ViewModel() {

    private val dinoId: Long = savedStateHandle.toRoute<Screen.DinoDetail>().dinoId
    private var profileId: Long = 1L

    private val _detail      = MutableStateFlow<DinoFullDetail?>(null)
    private val _level        = MutableStateFlow(26)
    private val _boosts       = MutableStateFlow(BoostState())
    private val _omegaPoints  = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _savedLevel   = MutableStateFlow(26)
    private val _savedBoosts  = MutableStateFlow(BoostState())
    private val _savedOmega   = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _dnaOnHand    = MutableStateFlow(0)
    private val _isNew        = MutableStateFlow(false)
    private val _enhancementItems = MutableStateFlow<List<StoredEnhancement>>(emptyList())

    val pendingEnhancementUncheck = MutableStateFlow<PendingEnhancementUncheck?>(null)

    private val _saveEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveEvents: SharedFlow<Unit> = _saveEvents.asSharedFlow()

    private val isOmega: Boolean
        get() = _detail.value?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT

    val uiState: StateFlow<DinoDetailUiState> = combine(
        combine(_detail, _level, _boosts, _omegaPoints) { detail, level, boosts, omega ->
            detail to Inputs(level, boosts, omega)
        },
        combine(_savedLevel, _savedBoosts, _savedOmega) { sl, sb, so -> Inputs(sl, sb, so) },
        _dnaOnHand,
        _isNew,
        _enhancementItems,
    ) { (detail, current), saved, dnaOnHand, isNew, rawEnhancements ->
        val (level, boosts, omegaPoints) = current
        val isOmega = detail?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT
        val hasUnsavedChanges = level != saved.level ||
            boosts != saved.boosts ||
            (isOmega && omegaPoints != saved.omegaPoints)

        val uiEnhancements = rawEnhancements.mapIndexed { i, raw ->
            val prevUnlocked = i == 0 || rawEnhancements[i - 1].isUnlocked
            EnhancementUiItem(
                id = raw.id,
                tier = raw.tier,
                description = raw.description,
                isUnlocked = raw.isUnlocked,
                isAvailable = level >= ENHANCEMENT_UNLOCK_LEVEL && prevUnlocked,
            )
        }

        val unlockedBonuses = rawEnhancements.filter { it.isUnlocked }.flatMap { it.statBonuses }
        val maxBoosts = StatCalculator.maxTotalBoosts(level) + rawEnhancements.boostCapBonus()

        fun applyBonuses(base: Int, stat: BoostStat): Int {
            var r = base.toDouble()
            for (b in unlockedBonuses) {
                if (b.stat == stat.dbKey) {
                    r = if (b.isPercentage) r * (1.0 + b.value / 100.0) else r + b.value
                }
            }
            return r.toInt()
        }

        val stats = detail?.stats
        val computed = if (stats != null) {
            if (isOmega) {
                val cfgMap = detail.omegaTrainingConfigs.associateBy { it.stat }
                // Training points raise the stat (capped at maxCap) first; boosts multiply the trained total.
                fun trained(base: Int, stat: OmegaStat): Int {
                    val cfg = cfgMap[stat.dbKey] ?: return base
                    return StatCalculator.applyOmegaTraining(base, omegaPoints[stat.dbKey] ?: 0, cfg.gainPerPoint, cfg.maxCap)
                }
                fun trainedF(base: Float, stat: OmegaStat): Float {
                    val cfg = cfgMap[stat.dbKey] ?: return base
                    return minOf(base + (omegaPoints[stat.dbKey] ?: 0) * cfg.gainPerPoint, cfg.maxCap.toFloat())
                }
                ComputedStats(
                    health = applyBonuses(StatCalculator.applyPercentBoost(trained(stats.baseHealth, OmegaStat.HEALTH), boosts.health), BoostStat.HEALTH),
                    attack = applyBonuses(StatCalculator.applyPercentBoost(trained(stats.baseAttack, OmegaStat.ATTACK), boosts.attack), BoostStat.ATTACK),
                    speed  = applyBonuses(StatCalculator.applySpeedBoost(trained(stats.speed, OmegaStat.SPEED), boosts.speed), BoostStat.SPEED),
                    armor          = trainedF(stats.armor, OmegaStat.ARMOR),
                    critChance     = trainedF(stats.critChance, OmegaStat.CRIT_CHANCE),
                    critMultiplier = trainedF(stats.critMultiplier, OmegaStat.CRIT_MULTIPLIER),
                )
            } else {
                ComputedStats(
                    health = applyBonuses(StatCalculator.applyPercentBoost(StatCalculator.scaleStat(stats.baseHealth, level), boosts.health), BoostStat.HEALTH),
                    attack = applyBonuses(StatCalculator.applyPercentBoost(StatCalculator.scaleStat(stats.baseAttack, level), boosts.attack), BoostStat.ATTACK),
                    speed  = applyBonuses(StatCalculator.applySpeedBoost(stats.speed, boosts.speed), BoostStat.SPEED),
                    armor          = stats.armor,
                    critChance     = stats.critChance,
                    critMultiplier = stats.critMultiplier,
                )
            }
        } else null

        DinoDetailUiState(
            detail            = detail,
            level             = level,
            boosts            = boosts,
            omegaPoints       = omegaPoints,
            computed          = computed,
            hasUnsavedChanges = hasUnsavedChanges,
            isLoading         = detail == null,
            dnaOnHand         = dnaOnHand,
            isNew             = isNew,
            enhancementItems  = uiEnhancements,
            maxTotalBoosts    = maxBoosts,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DinoDetailUiState())

    init {
        viewModelScope.launch {
            profileId = activeProfileRepository.requireActiveProfileId()

            val detail = detailRepository.getFullDetail(dinoId)
            _detail.value = detail

            val minLev = detail?.dino?.rarity?.minLevel() ?: 1
            val userDino = userDinoDao.getByDinoId(profileId, dinoId)
            val level = (userDino?.currentLevel ?: (detail?.dino?.rarity?.defaultLevel() ?: 26))
                .coerceAtLeast(minLev)
            _savedLevel.value = level
            _level.value = level

            val boosts = BoostState.fromRows(userBoostDao.getForDino(profileId, dinoId))
            _savedBoosts.value = boosts
            _boosts.value = boosts

            if (detail?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT) {
                val pts = omegaAllocationDao.getForDino(profileId, dinoId).associate { it.stat to it.pointsAllocated }
                _savedOmega.value = pts
                _omegaPoints.value = pts
            }

            _dnaOnHand.value = userDnaInventoryDao.get(profileId, dinoId)?.dnaAmount ?: 0

            val rarity = detail?.dino?.rarity
            if (rarity == Rarity.UNIQUE || rarity == Rarity.APEX) {
                val gameEnhancements = enhancementDao.getForDino(dinoId)
                val statBonuses = if (gameEnhancements.isNotEmpty())
                    enhancementDao.getStatBonuses(gameEnhancements.map { it.id })
                else emptyList()
                val bonusMap = statBonuses.groupBy { it.enhancementId }
                val unlockedIds = userEnhancementDao.getForDino(profileId, dinoId)
                    .filter { it.isUnlocked }.map { it.enhancementId }.toSet()
                _enhancementItems.value = gameEnhancements.map { e ->
                    StoredEnhancement(
                        id = e.id,
                        tier = e.enhancementTier,
                        description = e.description,
                        statBonuses = bonusMap[e.id] ?: emptyList(),
                        isUnlocked = e.id in unlockedIds,
                    )
                }
            }

            val slug = detail?.dino?.slug
            if (slug != null) {
                viewModelScope.launch {
                    newDinoDao.observeNewSlugs(profileId).collect { newSlugs ->
                        _isNew.value = slug in newSlugs
                    }
                }
            }
        }
    }

    fun clearNewStatus() {
        val slug = _detail.value?.dino?.slug ?: return
        viewModelScope.launch { newDinoDao.delete(profileId, slug) }
    }

    fun setDnaOnHand(dna: Int) {
        val clamped = dna.coerceAtLeast(0)
        _dnaOnHand.value = clamped
        viewModelScope.launch {
            userDnaInventoryDao.upsert(UserDnaInventory(profileId = profileId, dinoId = dinoId, dnaAmount = clamped))
        }
    }

    fun setLevel(level: Int) {
        val minLev = _detail.value?.dino?.rarity?.minLevel() ?: StatCalculator.MIN_LEVEL
        val clamped = level.coerceIn(minLev, StatCalculator.MAX_LEVEL)
        _level.value = clamped
        if (isOmega) {
            val totalAvail = StatCalculator.maxOmegaTrainingPoints(clamped)
            val cur = _omegaPoints.value
            if (cur.values.sum() > totalAvail) _omegaPoints.value = clampOmegaPoints(cur, totalAvail)
        } else {
            val cap = currentMaxTotalBoosts()
            if (_boosts.value.total > cap) _boosts.value = _boosts.value.clampedTo(cap)
        }
    }

    // ── Boost setters (non-Omega dinos) ──────────────────────────────────────

    fun setBoost(stat: BoostStat, tiers: Int) {
        val b = _boosts.value
        _boosts.value = b.with(stat, tiers.coerceIn(0, b.maxFor(stat, currentMaxTotalBoosts())))
    }

    // ── Enhancement toggle ────────────────────────────────────────────────────

    fun toggleEnhancement(item: EnhancementUiItem) {
        if (!item.isAvailable) return
        val items = _enhancementItems.value
        if (item.isUnlocked) {
            val toUncheck = items.filter { it.isUnlocked && it.tier >= item.tier }
            val lostBoostBonus = toUncheck.flatMap { it.statBonuses }
                .filter { it.stat == BONUS_MAX_BOOSTS && !it.isPercentage }
                .sumOf { it.value.toInt() }
            val newMax = currentMaxTotalBoosts() - lostBoostBonus
            val boostsTrimmed = maxOf(0, _boosts.value.total - newMax)
            if (boostsTrimmed > 0) {
                pendingEnhancementUncheck.value = PendingEnhancementUncheck(
                    tier = item.tier,
                    cascadeTiers = toUncheck.map { it.tier },
                    boostsTrimmed = boostsTrimmed,
                )
                return
            }
            applyUncheck(toUncheck)
        } else {
            applyCheck(item.id)
        }
    }

    fun confirmEnhancementUncheck() {
        val pending = pendingEnhancementUncheck.value ?: return
        val toUncheck = _enhancementItems.value.filter { it.tier in pending.cascadeTiers }
        applyUncheck(toUncheck)
        pendingEnhancementUncheck.value = null
    }

    fun cancelEnhancementUncheck() {
        pendingEnhancementUncheck.value = null
    }

    private fun applyCheck(id: Long) {
        _enhancementItems.value = _enhancementItems.value.map {
            if (it.id == id) it.copy(isUnlocked = true) else it
        }
        viewModelScope.launch {
            userEnhancementDao.upsert(
                UserDinoEnhancement(profileId, dinoId, id, true, System.currentTimeMillis())
            )
        }
    }

    private fun applyUncheck(toUncheck: List<StoredEnhancement>) {
        val uncheckIds = toUncheck.map { it.id }.toSet()
        _enhancementItems.value = _enhancementItems.value.map {
            if (it.id in uncheckIds) it.copy(isUnlocked = false) else it
        }
        val newMax = currentMaxTotalBoosts()
        val currentBoosts = _boosts.value
        if (currentBoosts.total > newMax) {
            val trimmed = currentBoosts.clampedTo(newMax)
            _boosts.value = trimmed
            _savedBoosts.value = trimmed
            viewModelScope.launch { userBoostDao.insertAll(trimmed.toRows(profileId, dinoId)) }
        }
        viewModelScope.launch {
            toUncheck.forEach { e ->
                userEnhancementDao.upsert(UserDinoEnhancement(profileId, dinoId, e.id, false, null))
            }
        }
    }

    // ── Omega training point setter ───────────────────────────────────────────

    fun setOmegaPoints(stat: String, value: Int) {
        val cfgMap = _detail.value?.omegaTrainingConfigs?.associateBy { it.stat } ?: return
        val totalAvail = StatCalculator.maxOmegaTrainingPoints(_level.value)
        val current = _omegaPoints.value
        val otherTotal = current.entries.filter { it.key != stat }.sumOf { it.value }
        val maxForStat = minOf(cfgMap[stat]?.pointCap ?: 0, totalAvail - otherTotal)
        _omegaPoints.value = current + (stat to value.coerceIn(0, maxForStat))
    }

    // ── Reset / Save ──────────────────────────────────────────────────────────

    /** Revert unsaved edits to the last saved state. */
    fun reset() {
        _level.value = _savedLevel.value
        _boosts.value = _savedBoosts.value
        if (isOmega) _omegaPoints.value = _savedOmega.value
    }

    /** Clear level, boosts and training points back to the rarity defaults (unsaved until [save]). */
    fun fullReset() {
        _level.value = _detail.value?.dino?.rarity?.defaultLevel() ?: 26
        _boosts.value = BoostState()
        if (isOmega) _omegaPoints.value = emptyMap()
    }

    fun save() {
        val level       = _level.value
        val boosts      = _boosts.value
        val omegaPoints = _omegaPoints.value
        val omega       = isOmega
        viewModelScope.launch {
            userDinoDao.upsert(UserDino(profileId = profileId, dinoId = dinoId, currentLevel = level))
            userBoostDao.insertAll(boosts.toRows(profileId, dinoId))
            _savedBoosts.value = boosts
            if (omega) {
                omegaAllocationDao.insertAll(OmegaStat.entries.map { stat ->
                    OmegaTrainingAllocation(profileId, dinoId, stat.dbKey, omegaPoints[stat.dbKey] ?: 0)
                })
                _savedOmega.value = omegaPoints
            }
            _savedLevel.value = level
            _saveEvents.emit(Unit)
        }
    }

    private fun currentMaxTotalBoosts(): Int =
        StatCalculator.maxTotalBoosts(_level.value) + _enhancementItems.value.boostCapBonus()

    private fun clampOmegaPoints(current: Map<String, Int>, totalAvail: Int): Map<String, Int> {
        var rem = totalAvail
        return OmegaStat.entries.associate { stat ->
            val alloc = minOf(current[stat.dbKey] ?: 0, rem)
            rem -= alloc
            stat.dbKey to alloc
        }
    }
}
