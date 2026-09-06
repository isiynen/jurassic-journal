package com.sufficienteffort.jurassicjournal.ui.sanctuary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoDao
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoSanctuaryPointDao
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.UserBoostDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoDao
import com.sufficienteffort.jurassicjournal.util.StatCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SanctuaryBoostConfig(
    val speed: Int = 0,
    val attack: Int = 0,
    val health: Int = 0,
) {
    val total: Int get() = speed + attack + health
}

data class SanctuaryUiState(
    val isLoading: Boolean = true,
    val dino: Dino? = null,
    val level: Int = 26,
    val boosts: SanctuaryBoostConfig = SanctuaryBoostConfig(),
    val estimatedSpPerAction: Int? = null,
)

@HiltViewModel
class SanctuaryCalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dinoDao: DinoDao,
    private val sanctuaryPointDao: DinoSanctuaryPointDao,
    private val activeProfileRepository: ActiveProfileRepository,
    private val userDinoDao: UserDinoDao,
    private val userBoostDao: UserBoostDao,
) : ViewModel() {

    private val dinoId: Long = checkNotNull(savedStateHandle["dinoId"])

    private val _uiState = MutableStateFlow(SanctuaryUiState())
    val uiState: StateFlow<SanctuaryUiState> = _uiState.asStateFlow()

    // Loaded once in init; SP data is static per dino, so stepper ticks
    // shouldn't re-query Room.
    private var spSad: Double? = null

    init {
        viewModelScope.launch {
            val profileId = activeProfileRepository.activeProfileId.first()
            val dino = dinoDao.getById(dinoId) ?: return@launch
            spSad = sanctuaryPointDao.getForDino(dinoId)?.spSad

            val minLev = dino.rarity.minLevel()
            val userDino = userDinoDao.getByDinoId(profileId, dinoId)
            val startLevel = (userDino?.currentLevel ?: maxOf(minLev, 26)).coerceAtLeast(minLev)

            val savedBoostList = userBoostDao.getForDino(profileId, dinoId)
            val startBoosts = SanctuaryBoostConfig(
                speed  = savedBoostList.firstOrNull { it.stat == "speed"  }?.boostsApplied ?: 0,
                attack = savedBoostList.firstOrNull { it.stat == "attack" }?.boostsApplied ?: 0,
                health = savedBoostList.firstOrNull { it.stat == "health" }?.boostsApplied ?: 0,
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    dino = dino,
                    level = startLevel,
                    boosts = startBoosts,
                    estimatedSpPerAction = spSad?.let { sad -> computeSp(sad, startLevel, startBoosts) },
                )
            }
        }
    }

    fun setLevel(level: Int) = updateState { state ->
        val clamped = level.coerceIn(state.dino?.rarity?.minLevel() ?: 1, 35)
        // Lowering the level lowers the boost cap; carry the boosts down with it
        // (same behavior as DinoDetailViewModel.setLevel).
        val cap = StatCalculator.maxTotalBoosts(clamped)
        val boosts = if (state.boosts.total > cap) clampBoosts(state.boosts, cap) else state.boosts
        state.copy(level = clamped, boosts = boosts)
    }

    private fun clampBoosts(b: SanctuaryBoostConfig, cap: Int): SanctuaryBoostConfig {
        var rem = cap
        val h = minOf(b.health, rem).also { rem -= it }
        val a = minOf(b.attack, rem).also { rem -= it }
        val s = minOf(b.speed,  rem)
        return SanctuaryBoostConfig(speed = s, attack = a, health = h)
    }

    fun setSpeedBoosts(value: Int)  = updateBoosts { it.copy(speed  = value.coerceIn(0, maxPerStat(it, it.speed, value))) }
    fun setAttackBoosts(value: Int) = updateBoosts { it.copy(attack = value.coerceIn(0, maxPerStat(it, it.attack, value))) }
    fun setHealthBoosts(value: Int) = updateBoosts { it.copy(health = value.coerceIn(0, maxPerStat(it, it.health, value))) }

    private fun maxPerStat(current: SanctuaryBoostConfig, oldValue: Int, newValue: Int): Int {
        val delta = newValue - oldValue
        val remaining = StatCalculator.maxTotalBoosts(_uiState.value.level) - current.total
        return if (delta > 0) minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT, oldValue + remaining)
               else StatCalculator.MAX_BOOST_TIERS_PER_STAT
    }

    private fun updateBoosts(transform: (SanctuaryBoostConfig) -> SanctuaryBoostConfig) {
        updateState { state -> state.copy(boosts = transform(state.boosts)) }
    }

    private fun updateState(transform: (SanctuaryUiState) -> SanctuaryUiState) {
        _uiState.update { state ->
            val next = transform(state)
            val sad = spSad
            if (sad == null) next
            else next.copy(estimatedSpPerAction = computeSp(sad, next.level, next.boosts))
        }
    }

    private fun computeSp(spSad: Double, level: Int, boosts: SanctuaryBoostConfig): Int =
        StatCalculator.calculateSp(spSad, level, boosts.health, boosts.attack, boosts.speed)
}
