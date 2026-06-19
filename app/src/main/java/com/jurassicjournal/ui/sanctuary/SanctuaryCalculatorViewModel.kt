package com.jurassicjournal.ui.sanctuary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jurassicjournal.data.game.dao.DinoDao
import com.jurassicjournal.data.game.dao.DinoSanctuaryPointDao
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.model.minLevel
import com.jurassicjournal.util.StatCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.pow

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
) : ViewModel() {

    private val dinoId: Long = checkNotNull(savedStateHandle["dinoId"])

    private val _uiState = MutableStateFlow(SanctuaryUiState())
    val uiState: StateFlow<SanctuaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val dino = dinoDao.getById(dinoId) ?: return@launch
            val sp = sanctuaryPointDao.getForDino(dinoId)
            val startLevel = maxOf(dino.rarity.minLevel(), 26)
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    dino = dino,
                    level = startLevel,
                    estimatedSpPerAction = sp?.let { computeSp(it.spSad, startLevel, state.boosts) },
                )
            }
        }
    }

    fun setLevel(level: Int) = updateState { it.copy(level = level) }

    fun setSpeedBoosts(value: Int)  = updateBoosts { it.copy(speed  = value.coerceIn(0, maxPerStat(it, it.speed, value))) }
    fun setAttackBoosts(value: Int) = updateBoosts { it.copy(attack = value.coerceIn(0, maxPerStat(it, it.attack, value))) }
    fun setHealthBoosts(value: Int) = updateBoosts { it.copy(health = value.coerceIn(0, maxPerStat(it, it.health, value))) }

    private fun maxPerStat(current: SanctuaryBoostConfig, oldValue: Int, newValue: Int): Int {
        val delta = newValue - oldValue
        val remaining = StatCalculator.MAX_BOOST_TIERS_TOTAL - current.total
        return if (delta > 0) minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT, oldValue + remaining)
               else StatCalculator.MAX_BOOST_TIERS_PER_STAT
    }

    private fun updateBoosts(transform: (SanctuaryBoostConfig) -> SanctuaryBoostConfig) {
        updateState { state -> state.copy(boosts = transform(state.boosts)) }
    }

    private fun updateState(transform: (SanctuaryUiState) -> SanctuaryUiState) {
        viewModelScope.launch {
            val sp = sanctuaryPointDao.getForDino(dinoId) ?: run {
                _uiState.update(transform)
                return@launch
            }
            _uiState.update { state ->
                val next = transform(state)
                next.copy(estimatedSpPerAction = computeSp(sp.spSad, next.level, next.boosts))
            }
        }
    }

    private fun computeSp(spSad: Double, level: Int, boosts: SanctuaryBoostConfig): Int {
        val a = spSad / (1.05.pow(26.0) * 1.25)
        val boostMult = 1.0 + (boosts.health + boosts.attack) * 0.0125 + boosts.speed * 0.02
        return floor(a * 1.05.pow(level.toDouble()) * boostMult).toInt()
    }
}
