package com.sufficienteffort.jurassicjournal.ui.enhancement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoDao
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnhancementEstimatorUiState(
    val isLoading: Boolean = true,
    val dino: Dino? = null,
    val isApex: Boolean = false,
    val current: Int = 0,
    val target: Int = 1,
    val result: EnhancementCostResult = EnhancementCostResult(),
)

data class EnhancementCostResult(
    val bronze: Int = 0,
    val silver: Int = 0,
    val gold: Int = 0,
    val coins: Int = 0,
    val dna: Int = 0,
)

@HiltViewModel
class EnhancementEstimatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dinoDao: DinoDao,
) : ViewModel() {

    private val dinoId: Long = checkNotNull(savedStateHandle["dinoId"])
    private val initialEnhancement: Int = savedStateHandle["currentEnhancement"] ?: 0

    private val _uiState = MutableStateFlow(EnhancementEstimatorUiState())
    val uiState: StateFlow<EnhancementEstimatorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val dino = dinoDao.getById(dinoId) ?: return@launch
            val isApex = dino.rarity == Rarity.APEX
            val current = initialEnhancement.coerceIn(0, 4)
            val target = (current + 1).coerceIn(1, 5)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    dino = dino,
                    isApex = isApex,
                    current = current,
                    target = target,
                    result = computeCost(isApex, current, target),
                )
            }
        }
    }

    fun setIsApex(apex: Boolean) = updateAndCompute { it.copy(isApex = apex) }

    fun setCurrent(value: Int) = updateAndCompute { state ->
        val clamped = value.coerceIn(0, 4)
        val newTarget = maxOf(state.target, clamped + 1).coerceIn(1, 5)
        state.copy(current = clamped, target = newTarget)
    }

    fun setTarget(value: Int) = updateAndCompute { state ->
        state.copy(target = value.coerceIn(state.current + 1, 5))
    }

    private fun updateAndCompute(transform: (EnhancementEstimatorUiState) -> EnhancementEstimatorUiState) {
        _uiState.update { state ->
            val next = transform(state)
            next.copy(result = computeCost(next.isApex, next.current, next.target))
        }
    }

    private fun computeCost(isApex: Boolean, current: Int, target: Int): EnhancementCostResult {
        val costs = if (isApex) APEX_COSTS else UNIQUE_COSTS
        var bronze = 0; var silver = 0; var gold = 0; var coins = 0; var dna = 0
        for (tier in (current + 1)..target) {
            val c = costs[tier - 1]
            bronze += c.bronze; silver += c.silver; gold += c.gold
            coins += c.coins; dna += c.dna
        }
        return EnhancementCostResult(bronze, silver, gold, coins, dna)
    }

    private data class TierCost(val bronze: Int, val silver: Int, val gold: Int, val coins: Int, val dna: Int)

    companion object {
        private val UNIQUE_COSTS = listOf(
            TierCost(bronze = 360,  silver = 0,    gold = 0,    coins = 100_000, dna = 200),
            TierCost(bronze = 1300, silver = 430,  gold = 0,    coins = 200_000, dna = 150),
            TierCost(bronze = 1080, silver = 1620, gold = 0,    coins = 250_000, dna = 200),
            TierCost(bronze = 0,    silver = 1940, gold = 650,  coins = 300_000, dna = 250),
            TierCost(bronze = 0,    silver = 0,    gold = 2000, coins = 350_000, dna = 500),
        )
        private val APEX_COSTS = listOf(
            TierCost(bronze = 360,  silver = 0,    gold = 0,    coins = 150_000, dna = 100),
            TierCost(bronze = 1300, silver = 430,  gold = 0,    coins = 200_000, dna = 150),
            TierCost(bronze = 1080, silver = 1620, gold = 0,    coins = 250_000, dna = 200),
            TierCost(bronze = 0,    silver = 1940, gold = 650,  coins = 300_000, dna = 250),
            TierCost(bronze = 0,    silver = 1020, gold = 1540, coins = 400_000, dna = 300),
        )
    }
}
