package com.jurassicjournal.ui.dino

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jurassicjournal.data.game.repository.DinoDetailRepository
import com.jurassicjournal.data.game.repository.DinoFullDetail
import com.jurassicjournal.data.model.ProgressionSystem
import com.jurassicjournal.data.model.minLevel
import com.jurassicjournal.data.user.ActiveProfileRepository
import com.jurassicjournal.data.user.dao.OmegaTrainingAllocationDao
import com.jurassicjournal.data.user.dao.UserBoostDao
import com.jurassicjournal.data.user.dao.UserDinoDao
import com.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.jurassicjournal.data.user.entity.OmegaTrainingAllocation
import com.jurassicjournal.data.user.entity.UserBoost
import com.jurassicjournal.data.user.entity.UserDino
import com.jurassicjournal.data.user.entity.UserDnaInventory
import kotlinx.coroutines.flow.first
import com.jurassicjournal.util.StatCalculator
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

data class DinoDetailUiState(
    val detail: DinoFullDetail? = null,
    val level: Int = 26,
    val boosts: BoostState = BoostState(),
    val omegaPoints: Map<String, Int> = emptyMap(),
    val computed: ComputedStats? = null,
    val hasUnsavedChanges: Boolean = false,
    val isLoading: Boolean = true,
    val dnaOnHand: Int = 0,
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

@HiltViewModel
class DinoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val detailRepository: DinoDetailRepository,
    private val activeProfileRepository: ActiveProfileRepository,
    private val userDinoDao: UserDinoDao,
    private val userBoostDao: UserBoostDao,
    private val omegaAllocationDao: OmegaTrainingAllocationDao,
    private val userDnaInventoryDao: UserDnaInventoryDao,
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

    private val _dnaOnHand = MutableStateFlow(0)

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
    ) { (mutable, detail), saved, dnaOnHand ->
        val (level, boosts, omegaPoints) = mutable
        val isOmega = detail?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT
        val hasUnsavedChanges = level != saved.level ||
            boosts != saved.boosts ||
            (isOmega && omegaPoints != saved.omegaPoints)

        val stats = detail?.stats
        val computed = if (stats != null) {
            if (isOmega) {
                val cfgMap = detail.omegaTrainingConfigs.associateBy { it.stat }
                fun trainingBonus(stat: String): Int = (omegaPoints[stat] ?: 0) * (cfgMap[stat]?.gainPerPoint ?: 0)
                ComputedStats(
                    health = StatCalculator.applyHealthBoost(
                        StatCalculator.scaleStat(stats.baseHealth, level), boosts.health
                    ) + trainingBonus("health"),
                    attack = StatCalculator.applyAttackBoost(
                        StatCalculator.scaleStat(stats.baseAttack, level), boosts.attack
                    ) + trainingBonus("attack"),
                    speed = StatCalculator.applySpeedBoost(
                        stats.speed, boosts.speed
                    ) + trainingBonus("speed"),
                    armor          = stats.armor + trainingBonus("armor"),
                    critChance     = stats.critChance + trainingBonus("crit_chance"),
                    critMultiplier = stats.critMultiplier + trainingBonus("crit_multiplier"),
                )
            } else {
                ComputedStats(
                    health = StatCalculator.applyHealthBoost(
                        StatCalculator.scaleStat(stats.baseHealth, level), boosts.health
                    ),
                    attack = StatCalculator.applyAttackBoost(
                        StatCalculator.scaleStat(stats.baseAttack, level), boosts.attack
                    ),
                    speed = StatCalculator.applySpeedBoost(
                        stats.speed, boosts.speed
                    ),
                    armor          = stats.armor,
                    critChance     = stats.critChance,
                    critMultiplier = stats.critMultiplier,
                )
            }
        } else null

        DinoDetailUiState(
            detail           = detail,
            level            = level,
            boosts           = boosts,
            omegaPoints      = omegaPoints,
            computed         = computed,
            hasUnsavedChanges = hasUnsavedChanges,
            isLoading        = detail == null,
            dnaOnHand        = dnaOnHand,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DinoDetailUiState())

    init {
        viewModelScope.launch {
            profileId = activeProfileRepository.activeProfileId.first()

            val detail = detailRepository.getFullDetail(dinoId)
            _detail.value = detail

            val minLev = detail?.dino?.rarity?.minLevel() ?: 1
            val userDino = userDinoDao.getByDinoId(profileId, dinoId)
            val level = (userDino?.currentLevel ?: maxOf(26, minLev)).coerceAtLeast(minLev)
            _savedLevel.value = level
            _level.value = level

            // Boosts apply to all dinos
            val savedBoostList = userBoostDao.getForDino(profileId, dinoId)
            val boosts = BoostState(
                health = savedBoostList.firstOrNull { it.stat == "health" }?.boostsApplied ?: 0,
                attack = savedBoostList.firstOrNull { it.stat == "attack" }?.boostsApplied ?: 0,
                speed  = savedBoostList.firstOrNull { it.stat == "speed"  }?.boostsApplied ?: 0,
            )
            _savedBoosts.value = boosts
            _boosts.value = boosts

            // Training points apply additionally for Omega dinos
            if (detail?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT) {
                val saved = omegaAllocationDao.getForDino(profileId, dinoId)
                val pts = saved.associate { it.stat to it.pointsAllocated }
                _savedOmega.value = pts
                _omegaPoints.value = pts
            }

            _dnaOnHand.value = userDnaInventoryDao.get(profileId, dinoId)?.dnaAmount ?: 0
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
            val cap = StatCalculator.maxTotalBoosts(clamped)
            val b = _boosts.value
            if (b.total > cap) _boosts.value = clampBoosts(b, cap)
        }
    }

    // ── Boost setters (non-Omega dinos) ──────────────────────────────────────

    fun setHealthBoosts(tiers: Int) = updateBoost { b ->
        val max = minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT,
            StatCalculator.maxTotalBoosts(_level.value) - b.attack - b.speed)
        b.copy(health = tiers.coerceIn(0, max))
    }

    fun setAttackBoosts(tiers: Int) = updateBoost { b ->
        val max = minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT,
            StatCalculator.maxTotalBoosts(_level.value) - b.health - b.speed)
        b.copy(attack = tiers.coerceIn(0, max))
    }

    fun setSpeedBoosts(tiers: Int) = updateBoost { b ->
        val max = minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT,
            StatCalculator.maxTotalBoosts(_level.value) - b.health - b.attack)
        b.copy(speed = tiers.coerceIn(0, max))
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
        val minLev = _detail.value?.dino?.rarity?.minLevel() ?: 1
        _level.value = maxOf(26, minLev)
        _boosts.value = BoostState()
        if (_detail.value?.dino?.progressionSystem == ProgressionSystem.TRAINING_POINT) {
            _omegaPoints.value = emptyMap()
        }
        // No auto-save — user must press Save or confirm on exit
    }

    fun save() = persist()

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
            // Boosts apply to all dinos
            userBoostDao.upsert(UserBoost(profileId, dinoId, "health", boosts.health))
            userBoostDao.upsert(UserBoost(profileId, dinoId, "attack", boosts.attack))
            userBoostDao.upsert(UserBoost(profileId, dinoId, "speed",  boosts.speed))
            _savedBoosts.value = boosts
            // Training points additionally for Omega dinos
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
