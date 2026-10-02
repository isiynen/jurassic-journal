package com.sufficienteffort.jurassicjournal.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sufficienteffort.jurassicjournal.data.game.dao.LevelUpCostDao
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.game.entity.LevelUpCost
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoDetailRepository
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserWalletDao
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDnaInventory
import com.sufficienteffort.jurassicjournal.data.user.entity.UserWallet
import com.sufficienteffort.jurassicjournal.ui.navigation.Screen
import com.sufficienteffort.jurassicjournal.util.StatCalculator
import com.sufficienteffort.jurassicjournal.util.rangeCost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LevelUpResult(
    val dnaStillNeeded: Long,
    val coinsNeeded: Long,
    val coinDeficit: Long,
)

data class LevelUpCalculatorUiState(
    val dino: Dino? = null,
    val minLevel: Int = 1,
    val currentLevel: Int = 0,
    val targetLevel: Int = 1,
    val dnaOnHand: Int = 0,
    val coinsOnHand: Long = 0L,
    val result: LevelUpResult? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class LevelUpCalculatorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val detailRepository: DinoDetailRepository,
    private val levelUpCostDao: LevelUpCostDao,
    private val activeProfileRepository: ActiveProfileRepository,
    private val userDinoDao: UserDinoDao,
    private val userDnaInventoryDao: UserDnaInventoryDao,
    private val userWalletDao: UserWalletDao,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.LevelUpCalculator>()
    private val dinoId: Long = route.dinoId
    private val navCurrentLevel: Int = route.currentLevel
    private var profileId: Long = 1L

    private val _dinoData     = MutableStateFlow<Pair<Dino, List<LevelUpCost>>?>(null)
    private val _currentLevel = MutableStateFlow(0)
    private val _targetLevel  = MutableStateFlow(1)
    private val _dnaOnHand    = MutableStateFlow(0)
    private val _coinsOnHand  = MutableStateFlow(0L)

    val uiState: StateFlow<LevelUpCalculatorUiState> = combine(
        _dinoData,
        _currentLevel,
        _targetLevel,
        _dnaOnHand,
        _coinsOnHand,
    ) { dinoData, currentLevel, targetLevel, dnaOnHand, coinsOnHand ->
        val (dino, costs) = dinoData ?: return@combine LevelUpCalculatorUiState(isLoading = true)

        val result = if (targetLevel > currentLevel) {
            val range = costs.rangeCost(currentLevel, targetLevel)
            LevelUpResult(
                dnaStillNeeded = maxOf(0L, range.dna - dnaOnHand),
                coinsNeeded    = range.coins,
                coinDeficit    = maxOf(0L, range.coins - coinsOnHand),
            )
        } else null

        LevelUpCalculatorUiState(
            dino         = dino,
            minLevel     = dino.rarity.minLevel(),
            currentLevel = currentLevel,
            targetLevel  = targetLevel,
            dnaOnHand    = dnaOnHand,
            coinsOnHand  = coinsOnHand,
            result       = result,
            isLoading    = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LevelUpCalculatorUiState())

    init {
        viewModelScope.launch {
            profileId = activeProfileRepository.requireActiveProfileId()

            val detail = detailRepository.getFullDetail(dinoId) ?: return@launch
            val costs  = levelUpCostDao.getForRarity(detail.dino.rarity)
            _dinoData.value = detail.dino to costs

            // "Unlock" is one below the rarity's first level and includes the creation DNA cost.
            val minLev         = detail.dino.rarity.minLevel()
            val unlockLevel    = minLev - 1
            val passedLevel    = if (navCurrentLevel >= minLev) navCurrentLevel else null
            val savedUserLevel = userDinoDao.getByDinoId(profileId, dinoId)?.currentLevel
            val currentLev     = passedLevel?.coerceIn(unlockLevel, StatCalculator.MAX_LEVEL)
                ?: savedUserLevel?.coerceAtLeast(unlockLevel)
                ?: unlockLevel
            _currentLevel.value = currentLev
            _targetLevel.value  = if (currentLev >= minLev) (currentLev + 1).coerceAtMost(StatCalculator.MAX_LEVEL) else minLev

            _dnaOnHand.value   = userDnaInventoryDao.get(profileId, dinoId)?.dnaAmount ?: 0
            _coinsOnHand.value = userWalletDao.get(profileId)?.coins ?: 0L
        }
    }

    fun setCurrentLevel(level: Int) {
        val unlockLevel = (_dinoData.value?.first?.rarity?.minLevel() ?: 1) - 1
        val clamped = level.coerceIn(unlockLevel, StatCalculator.MAX_LEVEL - 1)
        _currentLevel.value = clamped
        if (_targetLevel.value <= clamped) {
            _targetLevel.value = (clamped + 1).coerceAtMost(StatCalculator.MAX_LEVEL)
        }
    }

    fun setTargetLevel(level: Int) {
        _targetLevel.value = level.coerceIn(_currentLevel.value + 1, StatCalculator.MAX_LEVEL)
    }

    fun setDnaOnHand(dna: Int) {
        val clamped = dna.coerceAtLeast(0)
        _dnaOnHand.value = clamped
        viewModelScope.launch {
            userDnaInventoryDao.upsert(UserDnaInventory(profileId = profileId, dinoId = dinoId, dnaAmount = clamped))
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
