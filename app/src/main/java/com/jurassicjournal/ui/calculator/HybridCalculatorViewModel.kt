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
    val isCreate: Boolean = false,
    val currentLevel: Int = 0,
    val targetLevel: Int = 1,
    val currentHybridDna: Int = 0,
    val coinsOnHand: Long = 0,
    val ingredients: List<IngredientInput> = emptyList(),
    val result: CalcResult? = null,
    val maxReachableLevel: Int? = null,
    val isLoading: Boolean = true,
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
    private val _isCreate      = MutableStateFlow(false)
    private val _currentLevel  = MutableStateFlow(0)
    private val _targetLevel   = MutableStateFlow(1)
    private val _currentHybridDna = MutableStateFlow(0)
    private val _ingredients   = MutableStateFlow<List<IngredientInput>>(emptyList())
    private val _coinsOnHand   = MutableStateFlow(0L)

    val uiState: StateFlow<HybridCalculatorUiState> = combine(
        combine(_hybridData, _isCreate, _currentLevel) { hd, ic, cl -> Triple(hd, ic, cl) },
        combine(_targetLevel, _currentHybridDna, _ingredients) { tl, cd, ing -> Triple(tl, cd, ing) },
        _coinsOnHand,
    ) { (hybridData, isCreate, currentLevel), (targetLevel, currentHybridDna, ingredients), coinsOnHand ->
        val (hybrid, costs) = hybridData ?: return@combine HybridCalculatorUiState(isLoading = true)

        val result = if (targetLevel >= currentLevel) {
            calculateCosts(isCreate, hybrid.rarity, currentLevel, targetLevel, currentHybridDna, ingredients, costs, coinsOnHand)
        } else null

        val maxReachableLevel = calculateMaxReachableLevel(
            isCreate, hybrid.rarity, currentLevel, currentHybridDna, ingredients, coinsOnHand, costs,
        )

        HybridCalculatorUiState(
            hybrid            = hybrid,
            isCreate          = isCreate,
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

    fun setIsCreate(create: Boolean) {
        _isCreate.value = create
        val minLev = _hybridData.value?.first?.rarity?.minLevel() ?: 1
        if (create) {
            _currentLevel.value = minLev
            _targetLevel.value  = minLev
        } else {
            _targetLevel.value = (_currentLevel.value + 1).coerceAtMost(35)
        }
    }

    fun setCurrentLevel(level: Int) {
        if (_isCreate.value) return
        val minLev  = _hybridData.value?.first?.rarity?.minLevel() ?: 1
        val clamped = level.coerceIn(minLev, 34)
        _currentLevel.value = clamped
        if (_targetLevel.value <= clamped) {
            _targetLevel.value = (clamped + 1).coerceAtMost(35)
        }
        _currentHybridDna.value = 0
    }

    fun setTargetLevel(level: Int) {
        val minTarget = if (_isCreate.value) _currentLevel.value else _currentLevel.value + 1
        _targetLevel.value = level.coerceIn(minTarget, 35)
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
        isCreate: Boolean,
        rarity: Rarity,
        currentLevel: Int,
        targetLevel: Int,
        currentHybridDna: Int,
        ingredients: List<IngredientInput>,
        costs: List<LevelUpCost>,
        coinsOnHand: Long,
    ): CalcResult {
        val costMap = costs.associateBy { it.fromLevel }

        val creationDna = if (isCreate) creationDnaCostForRarity(rarity).toLong() else 0L
        val totalHybridDna = creationDna + (currentLevel until targetLevel).sumOf { level ->
            costMap[level]?.dnaCost?.toLong() ?: 0L
        }
        val remainingHybridDna = maxOf(0L, totalHybridDna - currentHybridDna)
        val fusesNeeded = ceil(remainingHybridDna / 20.0).toInt()

        val levelUpCoins = (currentLevel until targetLevel).sumOf { level ->
            costMap[level]?.coinsCost?.toLong() ?: 0L
        }
        val fuseCoins = fusesNeeded.toLong() * fuseCoinCostForRarity(rarity)
        val totalCoins = levelUpCoins + fuseCoins

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
        isCreate: Boolean,
        rarity: Rarity,
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

        if (isCreate) {
            val creationDnaNeeded = creationDnaCostForRarity(rarity).toLong()
            val deficit           = maxOf(0L, creationDnaNeeded - hybridDnaAvail)
            val fusesNeeded       = if (deficit > 0L) ceil(deficit / 20.0).toInt() else 0
            val fuseCoinsNeeded   = fusesNeeded.toLong() * fuseCoinCostForRarity(rarity)

            var canAfford = coinsAvail >= fuseCoinsNeeded
            for (i in ingredients.indices) {
                val needed = fusesNeeded.toLong() * fuseCostForRarity(ingredients[i].dino.rarity)
                if (ingredientDnaAvail[i] < needed) { canAfford = false; break }
            }
            if (!canAfford) return currentLevel - 1

            coinsAvail -= fuseCoinsNeeded
            for (i in ingredients.indices) {
                ingredientDnaAvail[i] -= fusesNeeded.toLong() * fuseCostForRarity(ingredients[i].dino.rarity)
            }
            hybridDnaAvail = hybridDnaAvail + fusesNeeded * 20L - creationDnaNeeded
        }

        for (fromLevel in currentLevel until 35) {
            val cost = costMap[fromLevel] ?: break

            val hybridDnaNeeded = cost.dnaCost.toLong()
            val deficit         = maxOf(0L, hybridDnaNeeded - hybridDnaAvail)
            val fusesNeeded     = if (deficit > 0L) ceil(deficit / 20.0).toInt() else 0
            val fuseCoinsNeeded = fusesNeeded.toLong() * fuseCoinCostForRarity(rarity)
            val totalCoinsNeeded = cost.coinsCost + fuseCoinsNeeded
            if (coinsAvail < totalCoinsNeeded) break

            var canAfford = true
            for (i in ingredients.indices) {
                val needed = fusesNeeded.toLong() * fuseCostForRarity(ingredients[i].dino.rarity)
                if (ingredientDnaAvail[i] < needed) { canAfford = false; break }
            }
            if (!canAfford) break

            coinsAvail -= totalCoinsNeeded
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

        fun creationDnaCostForRarity(rarity: Rarity): Int = when (rarity) {
            Rarity.RARE      -> 100
            Rarity.EPIC      -> 150
            Rarity.LEGENDARY -> 200
            Rarity.UNIQUE    -> 250
            Rarity.APEX      -> 300
            else             -> 0
        }

        // Coin cost charged per press of the "Fuse" button, by the hybrid's own rarity.
        // Rare/Epic/Legendary/Unique/Apex confirmed. Common is unused in practice —
        // hybrids only exist at Rare rarity and above.
        fun fuseCoinCostForRarity(rarity: Rarity): Long = when (rarity) {
            Rarity.COMMON    -> 20L
            Rarity.RARE      -> 20L
            Rarity.EPIC      -> 100L
            Rarity.LEGENDARY -> 200L
            Rarity.UNIQUE    -> 1_000L
            Rarity.APEX      -> 2_000L
            else             -> 0L
        }
    }
}
