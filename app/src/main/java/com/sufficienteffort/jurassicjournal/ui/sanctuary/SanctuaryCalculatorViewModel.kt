package com.sufficienteffort.jurassicjournal.ui.sanctuary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoDao
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoSanctuaryPointDao
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.model.BoostStat
import com.sufficienteffort.jurassicjournal.data.model.BoostState
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.UserBoostDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoDao
import com.sufficienteffort.jurassicjournal.ui.navigation.Screen
import com.sufficienteffort.jurassicjournal.util.StatCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SanctuaryUiState(
    val isLoading: Boolean = true,
    val dino: Dino? = null,
    val level: Int = 26,
    val boosts: BoostState = BoostState(),
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

    private val dinoId: Long = savedStateHandle.toRoute<Screen.SanctuaryCalculator>().dinoId

    private val _uiState = MutableStateFlow(SanctuaryUiState())
    val uiState: StateFlow<SanctuaryUiState> = _uiState.asStateFlow()

    // Loaded once in init; SP data is static per dino, so stepper ticks
    // shouldn't re-query Room.
    private var spSad: Double? = null

    init {
        viewModelScope.launch {
            val profileId = activeProfileRepository.requireActiveProfileId()
            val dino = dinoDao.getById(dinoId) ?: return@launch
            spSad = sanctuaryPointDao.getForDino(dinoId)?.spSad

            val minLev = dino.rarity.minLevel()
            val userDino = userDinoDao.getByDinoId(profileId, dinoId)
            val startLevel = (userDino?.currentLevel ?: maxOf(minLev, 26)).coerceAtLeast(minLev)
            val startBoosts = BoostState.fromRows(userBoostDao.getForDino(profileId, dinoId))

            _uiState.update {
                it.copy(
                    isLoading = false,
                    dino = dino,
                    level = startLevel,
                    boosts = startBoosts,
                    estimatedSpPerAction = spSad?.let { sad -> StatCalculator.calculateSp(sad, startLevel, startBoosts) },
                )
            }
        }
    }

    fun setLevel(level: Int) = updateState { state ->
        val clamped = level.coerceIn(state.dino?.rarity?.minLevel() ?: StatCalculator.MIN_LEVEL, StatCalculator.MAX_LEVEL)
        // Lowering the level lowers the boost cap; carry the boosts down with it
        // (same behavior as DinoDetailViewModel.setLevel).
        val cap = StatCalculator.maxTotalBoosts(clamped)
        state.copy(level = clamped, boosts = state.boosts.clampedTo(cap))
    }

    fun setBoost(stat: BoostStat, value: Int) = updateState { state ->
        val cap = StatCalculator.maxTotalBoosts(state.level)
        state.copy(boosts = state.boosts.with(stat, value.coerceIn(0, state.boosts.maxFor(stat, cap))))
    }

    private fun updateState(transform: (SanctuaryUiState) -> SanctuaryUiState) {
        _uiState.update { state ->
            val next = transform(state)
            val sad = spSad
            if (sad == null) next
            else next.copy(estimatedSpPerAction = StatCalculator.calculateSp(sad, next.level, next.boosts))
        }
    }
}
