package com.jurassicjournal.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jurassicjournal.data.game.dao.LevelUpCostDao
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.game.entity.LevelUpCost
import com.jurassicjournal.data.game.repository.DinoDetailRepository
import com.jurassicjournal.data.model.Rarity
import com.jurassicjournal.data.model.minLevel
import com.jurassicjournal.data.user.ActiveProfileRepository
import com.jurassicjournal.data.user.dao.UserDinoDao
import com.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.jurassicjournal.data.user.dao.UserWalletDao
import com.jurassicjournal.data.user.entity.UserDnaInventory
import com.jurassicjournal.data.user.entity.UserWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

data class IngredientInput(
    val dino: Dino,
    val dnaOnHand: Int = 0,
)

data class IngredientCost(
    val dino: Dino,
    val dnaCostPerFuse: Int,
    val totalDnaNeeded: Long,
    val dnaOnHand: Int,
    val dnaDeficit: Long,
)

data class CalcResult(
    val hybridDnaStillNeeded: Long,
    val fusesNeeded: Int,
    val coinsNeeded: Long,
    val coinDeficit: Long,
    val ingredientCosts: List<IngredientCost>,
)

data class HybridCalculatorUiState(
    val hybrid: Dino? = null,
    val currentLevel: Int = 0,
    val targetLevel: Int = 1,
    val currentHybridDna: Int = 0,
    val coinsOnHand: Long = 0,
    val ingredients: List<IngredientInput> = emptyList(),
    val result: CalcResult? = null,
    val maxReachableLevel: Int? = null,
    val isLoading: Boolean = true,
)

private data class CalcCore(
    val hybridData: Pair<Dino, List<LevelUpCost>>?,
    val currentLevel: Int,
    val targetLevel: Int,
    val currentHybridDna: Int,
    val ingredients: List<IngredientInput>,
)

@HiltViewModel
class HybridCalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val detailRepository: DinoDetailRepository,
    private val levelUpCostDao: LevelUpCostDao,
    private val activeProfileRepository: ActiveProfileRepository,
    private val userDinoDao: UserDinoDao,
    private val userDnaInventoryDao: UserDnaInventoryDao,
    private val userWalletDao: UserWalletDao,
) : ViewModel() {

    private val dinoId: Long = checkNotNull(savedStateHandle["dinoId"])
    private var profileId: Long = 1L

    private val _hybridData    = MutableStateFlow<Pair<Dino, List<LevelUpCost>>?>(null)
    private val _currentLevel  = MutableStateFlow(0)
    private val _targetLevel   = MutableStateFlow(1)
    private val _currentHybridDna = MutableStateFlow(0)
    private val _ingredients   = MutableStateFlow<List<IngredientInput>>(emptyList())
    private val _coinsOnHand   = MutableStateFlow(0L)

    val uiState: StateFlow<HybridCalculatorUiState> = combine(
        combine(_hybridData, _currentLevel, _targetLevel, _currentHybridDna, _ingredients) {
            hd, cl, tl, cd, ing -> CalcCore(hd, cl, tl, cd, ing)
        },
        _coinsOnHand,
    ) { core, coinsOnHand ->
        val (hybridData, currentLevel, targetLevel, currentHybridDna, ingredients) = core
        val (hybrid, costs) = hybridData ?: return@combine HybridCalculatorUiState(isLoading = true)

        val result = if (targetLevel > currentLevel) {
            calculateCosts(currentLevel, targetLevel, currentHybridDna, ingredients, costs, coinsOnHand)
        } else null

        val maxReachableLevel = calculateMaxReachableLevel(
            currentLevel, currentHybridDna, ingredients, coinsOnHand, costs,
        )

        HybridCalculatorUiState(
            hybrid            = hybrid,
            currentLevel      = currentLevel,
            targetLevel       = targetLevel,
            currentHybridDna  = currentHybridDna,
            coinsOnHand       = coinsOnHand,
            ingredients       = ingredients,
            result            = result,
            maxReachableLevel = maxReachableLevel,
            isLoading         = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HybridCalculatorUiState())

    init {
        viewModelScope.launch {
            profileId = activeProfileRepository.activeProfileId.first()

            val detail = detailRepository.getFullDetail(dinoId) ?: return@launch
            val costs  = levelUpCostDao.getForRarity(detail.dino.rarity)
            _hybridData.value = detail.dino to costs

            val minLev = detail.dino.rarity.minLevel()
            val savedLevel = (userDinoDao.getByDinoId(profileId, dinoId)?.currentLevel ?: minLev)
                .coerceAtLeast(minLev)
            _currentLevel.value = savedLevel
            _targetLevel.value  = (savedLevel + 1).coerceAtMost(35)

            _currentHybridDna.value = userDnaInventoryDao.get(profileId, dinoId)?.dnaAmount ?: 0

            val ingredientIds  = detail.ingredientTree.map { it.dino.id }
            val savedDnaMap    = userDnaInventoryDao.getForDinos(profileId, ingredientIds).associateBy { it.dinoId }
            _ingredients.value = detail.ingredientTree.map { node ->
                IngredientInput(
                    dino      = node.dino,
                    dnaOnHand = savedDnaMap[node.dino.id]?.dnaAmount ?: 0,
                )
            }

            _coinsOnHand.value = userWalletDao.get(profileId)?.coins ?: 0L
        }
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setCurrentLevel(level: Int) {
        val minLev  = _hybridData.value?.first?.rarity?.minLevel() ?: 1
        val clamped = level.coerceIn(minLev, 34)
        _currentLevel.value = clamped
        if (_targetLevel.value <= clamped) {
            _targetLevel.value = (clamped + 1).coerceAtMost(35)
        }
        _currentHybridDna.value = 0
    }

    fun setTargetLevel(level: Int) {
        _targetLevel.value = level.coerceIn(_currentLevel.value + 1, 35)
    }

    fun setCurrentHybridDna(dna: Int) {
        val clamped = dna.coerceAtLeast(0)
        _currentHybridDna.value = clamped
        viewModelScope.launch {
            userDnaInventoryDao.upsert(UserDnaInventory(profileId = profileId, dinoId = dinoId, dnaAmount = clamped))
        }
    }

    fun setIngredientDna(index: Int, dna: Int) {
        val list = _ingredients.value.toMutableList()
        if (index in list.indices) {
            val clamped = dna.coerceAtLeast(0)
            val ingredientDinoId = list[index].dino.id
            list[index] = list[index].copy(dnaOnHand = clamped)
            _ingredients.value = list
            viewModelScope.launch {
                userDnaInventoryDao.upsert(UserDnaInventory(profileId = profileId, dinoId = ingredientDinoId, dnaAmount = clamped))
            }
        }
    }

    fun setCoinsOnHand(coins: Long) {
        val clamped = coins.coerceAtLeast(0L)
        _coinsOnHand.value = clamped
        viewModelScope.launch {
            userWalletDao.upsert(UserWallet(profileId = profileId, coins = clamped))
        }
    }

    // ── Calculations ──────────────────────────────────────────────────────────

    private fun calculateCosts(
        currentLevel: Int,
        targetLevel: Int,
        currentHybridDna: Int,
        ingredients: List<IngredientInput>,
        costs: List<LevelUpCost>,
        coinsOnHand: Long,
    ): CalcResult {
        val costMap = costs.associateBy { it.fromLevel }

        val totalHybridDna = (currentLevel until targetLevel).sumOf { level ->
            costMap[level]?.dnaCost?.toLong() ?: 0L
        }
        val remainingHybridDna = maxOf(0L, totalHybridDna - currentHybridDna)
        val fusesNeeded = ceil(remainingHybridDna / 20.0).toInt()

        val totalCoins = (currentLevel until targetLevel).sumOf { level ->
            costMap[level]?.coinsCost?.toLong() ?: 0L
        }

        val ingredientCosts = ingredients.map { input ->
            val costPerFuse  = fuseCostForRarity(input.dino.rarity)
            val totalDnaNeeded = fusesNeeded.toLong() * costPerFuse
            IngredientCost(
                dino           = input.dino,
                dnaCostPerFuse = costPerFuse,
                totalDnaNeeded = totalDnaNeeded,
                dnaOnHand      = input.dnaOnHand,
                dnaDeficit     = maxOf(0L, totalDnaNeeded - input.dnaOnHand),
            )
        }

        return CalcResult(
            hybridDnaStillNeeded = remainingHybridDna,
            fusesNeeded          = fusesNeeded,
            coinsNeeded          = totalCoins,
            coinDeficit          = maxOf(0L, totalCoins - coinsOnHand),
            ingredientCosts      = ingredientCosts,
        )
    }

    /**
     * Iterates level-by-level from currentLevel, spending DNA and coins greedily,
     * and returns the highest level reachable with the current inventory.
     */
    private fun calculateMaxReachableLevel(
        currentLevel: Int,
        currentHybridDna: Int,
        ingredients: List<IngredientInput>,
        coinsOnHand: Long,
        costs: List<LevelUpCost>,
    ): Int {
        val costMap = costs.associateBy { it.fromLevel }
        var hybridDnaAvail     = currentHybridDna.toLong()
        val ingredientDnaAvail = ingredients.map { it.dnaOnHand.toLong() }.toMutableList()
        var coinsAvail         = coinsOnHand
        var maxLevel           = currentLevel

        for (fromLevel in currentLevel until 35) {
            val cost = costMap[fromLevel] ?: break
            if (coinsAvail < cost.coinsCost) break

            val hybridDnaNeeded = cost.dnaCost.toLong()
            val deficit         = maxOf(0L, hybridDnaNeeded - hybridDnaAvail)
            val fusesNeeded     = if (deficit > 0L) ceil(deficit / 20.0).toInt() else 0

            var canAfford = true
            for (i in ingredients.indices) {
                val needed = fusesNeeded.toLong() * fuseCostForRarity(ingredients[i].dino.rarity)
                if (ingredientDnaAvail[i] < needed) { canAfford = false; break }
            }
            if (!canAfford) break

            coinsAvail -= cost.coinsCost
            for (i in ingredients.indices) {
                ingredientDnaAvail[i] -= fusesNeeded.toLong() * fuseCostForRarity(ingredients[i].dino.rarity)
            }
            // leftover hybrid DNA carries forward into next level
            hybridDnaAvail = hybridDnaAvail + fusesNeeded * 20L - hybridDnaNeeded
            maxLevel = fromLevel + 1
        }

        return maxLevel
    }

    companion object {
        fun fuseCostForRarity(rarity: Rarity): Int = when (rarity) {
            Rarity.COMMON    -> 50
            Rarity.RARE      -> 100
            Rarity.EPIC      -> 150
            Rarity.LEGENDARY -> 200
            Rarity.UNIQUE    -> 250
            else             -> 0
        }
    }
}
