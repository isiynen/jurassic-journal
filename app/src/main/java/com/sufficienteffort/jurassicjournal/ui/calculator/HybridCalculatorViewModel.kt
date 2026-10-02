package com.sufficienteffort.jurassicjournal.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sufficienteffort.jurassicjournal.data.game.dao.LevelUpCostDao
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.game.entity.LevelUpCost
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoDetailRepository
import com.sufficienteffort.jurassicjournal.data.game.repository.IngredientNode
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserWalletDao
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDnaInventory
import com.sufficienteffort.jurassicjournal.data.user.entity.UserWallet
import com.sufficienteffort.jurassicjournal.ui.navigation.Screen
import com.sufficienteffort.jurassicjournal.util.CalcResult
import com.sufficienteffort.jurassicjournal.util.HybridCostCalculator
import com.sufficienteffort.jurassicjournal.util.IngredientInput
import com.sufficienteffort.jurassicjournal.util.StatCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val dinoId: Long = savedStateHandle.toRoute<Screen.HybridCalculator>().dinoId
    private var profileId: Long = 1L

    private val _hybridData    = MutableStateFlow<Pair<Dino, List<LevelUpCost>>?>(null)
    private val _isCreate      = MutableStateFlow(false)
    private val _currentLevel  = MutableStateFlow(0)
    private val _targetLevel   = MutableStateFlow(1)
    private val _currentHybridDna = MutableStateFlow(0)
    private val _ingredients   = MutableStateFlow<List<IngredientInput>>(emptyList())
    private val _coinsOnHand   = MutableStateFlow(0L)

    // Tree structure used for recursive cost calculations; set once in init.
    private var ingredientTree: List<IngredientNode> = emptyList()

    val uiState: StateFlow<HybridCalculatorUiState> = combine(
        combine(_hybridData, _isCreate, _currentLevel) { hd, ic, cl -> Triple(hd, ic, cl) },
        combine(_targetLevel, _currentHybridDna, _ingredients) { tl, cd, ing -> Triple(tl, cd, ing) },
        _coinsOnHand,
    ) { (hybridData, isCreate, currentLevel), (targetLevel, currentHybridDna, ingredients), coinsOnHand ->
        val (hybrid, costs) = hybridData ?: return@combine HybridCalculatorUiState(isLoading = true)

        val result = if (targetLevel >= currentLevel) {
            HybridCostCalculator.calculateCosts(
                isCreate, hybrid.rarity, currentLevel, targetLevel, currentHybridDna,
                ingredients, ingredientTree, costs, coinsOnHand,
            )
        } else null

        val maxReachableLevel = HybridCostCalculator.calculateMaxReachableLevel(
            isCreate, hybrid.rarity, currentLevel, currentHybridDna, ingredients, ingredientTree, coinsOnHand, costs,
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
            profileId = activeProfileRepository.requireActiveProfileId()

            val detail = detailRepository.getFullDetail(dinoId) ?: return@launch
            val costs  = levelUpCostDao.getForRarity(detail.dino.rarity)

            ingredientTree = detail.ingredientTree
            _hybridData.value = detail.dino to costs

            val minLev = detail.dino.rarity.minLevel()
            val savedLevel = (userDinoDao.getByDinoId(profileId, dinoId)?.currentLevel ?: minLev)
                .coerceAtLeast(minLev)
            _currentLevel.value = savedLevel
            _targetLevel.value  = (savedLevel + 1).coerceAtMost(StatCalculator.MAX_LEVEL)

            _currentHybridDna.value = userDnaInventoryDao.get(profileId, dinoId)?.dnaAmount ?: 0

            val flat = HybridCostCalculator.flatten(detail.ingredientTree, detail.dino.rarity)
            val savedDnaMap = userDnaInventoryDao.getForDinos(profileId, flat.map { it.dino.id })
                .associateBy { it.dinoId }
            _ingredients.value = flat.map { node ->
                node.copy(dnaOnHand = savedDnaMap[node.dino.id]?.dnaAmount ?: 0)
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
            _targetLevel.value = (_currentLevel.value + 1).coerceAtMost(StatCalculator.MAX_LEVEL)
        }
    }

    fun setCurrentLevel(level: Int) {
        if (_isCreate.value) return
        val minLev  = _hybridData.value?.first?.rarity?.minLevel() ?: 1
        val clamped = level.coerceIn(minLev, StatCalculator.MAX_LEVEL - 1)
        _currentLevel.value = clamped
        if (_targetLevel.value <= clamped) {
            _targetLevel.value = (clamped + 1).coerceAtMost(StatCalculator.MAX_LEVEL)
        }
        _currentHybridDna.value = 0
    }

    fun setTargetLevel(level: Int) {
        val minTarget = if (_isCreate.value) _currentLevel.value else _currentLevel.value + 1
        _targetLevel.value = level.coerceIn(minTarget, StatCalculator.MAX_LEVEL)
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
        if (index !in list.indices) return
        val clamped = dna.coerceAtLeast(0)
        val ingredientDinoId = list[index].dino.id
        list[index] = list[index].copy(dnaOnHand = clamped)
        _ingredients.value = list
        viewModelScope.launch {
            userDnaInventoryDao.upsert(UserDnaInventory(profileId = profileId, dinoId = ingredientDinoId, dnaAmount = clamped))
        }
    }

    fun setCoinsOnHand(coins: Long) {
        val clamped = coins.coerceAtLeast(0L)
        _coinsOnHand.value = clamped
        viewModelScope.launch {
            userWalletDao.upsert(UserWallet(profileId = profileId, coins = clamped))
        }
    }
}
