package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sufficienteffort.jurassicjournal.data.game.dao.EnhancementDao
import com.sufficienteffort.jurassicjournal.data.game.entity.EnhancementStatBonus
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoDetailRepository
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoFullDetail
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
import com.sufficienteffort.jurassicjournal.data.user.entity.UserBoost
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDino
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDinoEnhancement
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDnaInventory
import kotlinx.coroutines.flow.first
import com.sufficienteffort.jurassicjournal.util.StatCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val OMEGA_STAT_KEYS = listOf("health", "attack", "speed", "armor", "crit_chance", "crit_multiplier")

data class BoostState(
    val health: Int = 0,
    val attack: Int = 0,
    val speed: Int = 0,
) {
    val total get() = health + attack + speed
}

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

private data class MutableInputs(
    val level: Int,
    val boosts: BoostState,
    val omegaPoints: Map<String, Int>,
)

private data class SavedInputs(
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

    private val dinoId: Long = checkNotNull(savedStateHandle["dinoId"])
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

    val uiState: StateFlow<DinoDetailUiState> = combine(
        combine(_detail, _level, _boosts, _omegaPoints) { detail, level, boosts, omega ->
            MutableInputs(level, boosts, omega) to detail
        },
        combine(_savedLevel, _savedBoosts, _savedOmega) { sl, sb, so ->
            SavedInputs(sl, sb, so)
        },
        _dnaOnHand,
        _isNew,
        _enhancementItems,
    ) { (mutable, detail), saved, dnaOnHand, isNew, rawEnhancements ->
        val (level, boosts, omegaPoints) = mutable
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
                isAvailable = level >= 30 && prevUnlocked,
            )
        }

        val unlockedBonuses = rawEnhancements.filter { it.isUnlocked }.flatMap { it.statBonuses }
        val boostBonus = unlockedBonuses
            .filter { it.stat == "max_boosts" && !it.isPercentage }
            .sumOf { it.value.toInt() }
        val maxBoosts = StatCalculator.maxTotalBoosts(level) + boostBonus

        fun applyBonuses(base: Int, stat: String): Int {
            var r = base.toDouble()
            for (b in unlockedBonuses) {
                if (b.stat == stat) {
                    r = if (b.isPercentage) r * (1.0 + b.value / 100.0) else r + b.value
                }
            }
            return r.toInt()
        }

        val stats = detail?.stats
        val computed = if (stats != null) {
            if (isOmega) {
                val cfgMap = detail.omegaTrainingConfigs.associateBy { it.stat }
                fun trainingBonus(stat: String): Int = (omegaPoints[stat] ?: 0) * (cfgMap[stat]?.gainPerPoint ?: 0)
                ComputedStats(
                    health = applyBonuses(
                        StatCalculator.applyHealthBoost(stats.baseHealth, boosts.health) + trainingBonus("health"),
                        "health"
                    ),
                    attack = applyBonuses(
                        StatCalculator.applyAttackBoost(stats.baseAttack, boosts.attack) + trainingBonus("attack"),
                        "attack"
                    ),
                    speed = applyBonuses(
                        StatCalculator.applySpeedBoost(stats.speed, boosts.speed) + trainingBonus("speed"),
                        "speed"
                    ),
                    armor          = stats.armor + trainingBonus("armor"),
                    critChance     = stats.critChance + trainingBonus("crit_chance"),
                    critMultiplier = stats.critMultiplier + trainingBonus("crit_multiplier"),
                )
            } else {
                ComputedStats(
                    health = applyBonuses(
                        StatCalculator.applyHealthBoost(StatCalculator.scaleStat(stats.baseHealth, level), boosts.health),
                        "health"
                    ),
                    attack = applyBonuses(
                        StatCalculator.applyAttackBoost(StatCalculator.scaleStat(stats.baseAttack, level), boosts.attack),
                        "attack"
                    ),
                    speed = applyBonuses(
                        StatCalculator.applySpeedBoost(stats.speed, boosts.speed),
                        "speed"
                    ),
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
            profileId = activeProfileRepository.activeProfileId.first()

            val detail = detailRepository.getFullDetail(dinoId)
            _detail.value = detail

            val minLev = detail?.dino?.rarity?.minLevel() ?: 1
            val userDino = userDinoDao.getByDinoId(profileId, dinoId)
            val level = (userDino?.currentLevel ?: (detail?.dino?.rarity?.defaultLevel() ?: 26))
                .coerceAtLeast(minLev)
            _savedLevel.value = level
            _level.value = level

            val savedBoostList = userBoostDao.getForDino(profileId, dinoId)
            val boosts = BoostState(
                health = savedBoostList.firstOrNull { it.stat == "health" }?.boostsApplied ?: 0,
                attack = savedBoostList.firstOrNull { it.stat == "attack" }?.boostsApplied ?: 0,
                speed  = savedBoostList.firstOrNull { it.stat == "speed"  }?.boostsApplied ?: 0,
            )
            _savedBoosts.value = boosts
            _boosts.value = boosts

            if (detail?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT) {
                val saved = omegaAllocationDao.getForDino(profileId, dinoId)
                val pts = saved.associate { it.stat to it.pointsAllocated }
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
                val userEnhList = userEnhancementDao.getForDino(profileId, dinoId)
                val unlockedIds = userEnhList.filter { it.isUnlocked }.map { it.enhancementId }.toSet()
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
        viewModelScope.launch {
            newDinoDao.delete(profileId, slug)
        }
    }

    fun setDnaOnHand(dna: Int) {
        val clamped = dna.coerceAtLeast(0)
        _dnaOnHand.value = clamped
        viewModelScope.launch {
            userDnaInventoryDao.upsert(UserDnaInventory(profileId = profileId, dinoId = dinoId, dnaAmount = clamped))
        }
    }

    fun setLevel(level: Int) {
        val minLev = _detail.value?.dino?.rarity?.minLevel() ?: 1
        val clamped = level.coerceIn(minLev, 35)
        _level.value = clamped
        val isOmega = _detail.value?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT
        if (isOmega) {
            val totalAvail = StatCalculator.maxOmegaTrainingPoints(clamped)
            val cur = _omegaPoints.value
            if (cur.values.sum() > totalAvail) _omegaPoints.value = clampOmegaPoints(cur, totalAvail)
        } else {
            val cap = currentMaxTotalBoosts()
            val b = _boosts.value
            if (b.total > cap) _boosts.value = clampBoosts(b, cap)
        }
    }

    // ── Boost setters (non-Omega dinos) ──────────────────────────────────────

    fun setHealthBoosts(tiers: Int) = updateBoost { b ->
        val max = minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT,
            currentMaxTotalBoosts() - b.attack - b.speed)
        b.copy(health = tiers.coerceIn(0, max))
    }

    fun setAttackBoosts(tiers: Int) = updateBoost { b ->
        val max = minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT,
            currentMaxTotalBoosts() - b.health - b.speed)
        b.copy(attack = tiers.coerceIn(0, max))
    }

    fun setSpeedBoosts(tiers: Int) = updateBoost { b ->
        val max = minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT,
            currentMaxTotalBoosts() - b.health - b.attack)
        b.copy(speed = tiers.coerceIn(0, max))
    }

    // ── Enhancement toggle ────────────────────────────────────────────────────

    fun toggleEnhancement(item: EnhancementUiItem) {
        if (!item.isAvailable) return
        val items = _enhancementItems.value
        if (item.isUnlocked) {
            val toUncheck = items.filter { it.isUnlocked && it.tier >= item.tier }
            val lostBoostBonus = toUncheck.flatMap { it.statBonuses }
                .filter { it.stat == "max_boosts" && !it.isPercentage }
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
            val trimmed = clampBoosts(currentBoosts, newMax)
            _boosts.value = trimmed
            _savedBoosts.value = trimmed
            viewModelScope.launch {
                userBoostDao.upsert(UserBoost(profileId, dinoId, "health", trimmed.health))
                userBoostDao.upsert(UserBoost(profileId, dinoId, "attack", trimmed.attack))
                userBoostDao.upsert(UserBoost(profileId, dinoId, "speed",  trimmed.speed))
            }
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

    fun reset() {
        _level.value = _savedLevel.value
        _boosts.value = _savedBoosts.value
        if (_detail.value?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT) {
            _omegaPoints.value = _savedOmega.value
        }
    }

    fun fullReset() {
        _level.value = _detail.value?.dino?.rarity?.defaultLevel() ?: 26
        _boosts.value = BoostState()
        if (_detail.value?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT) {
            _omegaPoints.value = emptyMap()
        }
    }

    fun save() = persist()

    private fun currentMaxTotalBoosts(): Int {
        val boostBonus = _enhancementItems.value
            .filter { it.isUnlocked }
            .flatMap { it.statBonuses }
            .filter { it.stat == "max_boosts" && !it.isPercentage }
            .sumOf { it.value.toInt() }
        return StatCalculator.maxTotalBoosts(_level.value) + boostBonus
    }

    private fun updateBoost(transform: (BoostState) -> BoostState) {
        _boosts.value = transform(_boosts.value)
    }

    private fun persist() {
        val level       = _level.value
        val boosts      = _boosts.value
        val omegaPoints = _omegaPoints.value
        val isOmega     = _detail.value?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT
        viewModelScope.launch {
            userDinoDao.upsert(UserDino(profileId = profileId, dinoId = dinoId, currentLevel = level))
            userBoostDao.upsert(UserBoost(profileId, dinoId, "health", boosts.health))
            userBoostDao.upsert(UserBoost(profileId, dinoId, "attack", boosts.attack))
            userBoostDao.upsert(UserBoost(profileId, dinoId, "speed",  boosts.speed))
            _savedBoosts.value = boosts
            if (isOmega) {
                OMEGA_STAT_KEYS.forEach { stat ->
                    omegaAllocationDao.upsert(
                        OmegaTrainingAllocation(profileId, dinoId, stat, omegaPoints[stat] ?: 0)
                    )
                }
                _savedOmega.value = omegaPoints
            }
            _savedLevel.value = level
            _saveEvents.emit(Unit)
        }
    }

    private fun clampBoosts(b: BoostState, cap: Int): BoostState {
        var rem = cap
        val h = minOf(b.health, rem).also { rem -= it }
        val a = minOf(b.attack, rem).also { rem -= it }
        val s = minOf(b.speed,  rem)
        return BoostState(h, a, s)
    }

    private fun clampOmegaPoints(current: Map<String, Int>, totalAvail: Int): Map<String, Int> {
        var rem = totalAvail
        return OMEGA_STAT_KEYS.associateWith { stat ->
            val alloc = minOf(current[stat] ?: 0, rem)
            rem -= alloc
            alloc
        }
    }
}
