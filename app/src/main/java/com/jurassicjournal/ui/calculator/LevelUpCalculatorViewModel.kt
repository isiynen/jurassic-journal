package com.sufficienteffort.jurassicjournal.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    private val dinoId: Long = checkNotNull(savedStateHandle["dinoId"])
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
        val minLev  = dino.rarity.minLevel()
        val costMap = costs.associateBy { it.fromLevel }

        val result = if (targetLevel > currentLevel) {
            val totalDna = (currentLevel until targetLevel).sumOf { level ->
                costMap[level]?.dnaCost?.toLong() ?: 0L
            }
            val totalCoins = (currentLevel until targetLevel).sumOf { level ->
                costMap[level]?.coinsCost?.toLong() ?: 0L
            }
            LevelUpResult(
                dnaStillNeeded = maxOf(0L, totalDna - dnaOnHand),
                coinsNeeded    = totalCoins,
                coinDeficit    = maxOf(0L, totalCoins - coinsOnHand),
            )
        } else null

        LevelUpCalculatorUiState(
            dino         = dino,
            minLevel     = minLev,
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
            profileId = activeProfileRepository.activeProfileId.first()

            val detail = detailRepository.getFullDetail(dinoId) ?: return@launch
            val costs  = levelUpCostDao.getForRarity(detail.dino.rarity)
            _dinoData.value = detail.dino to costs

            val minLev     = detail.dino.rarity.minLevel()
            val savedLevel = (userDinoDao.getByDinoId(profileId, dinoId)?.currentLevel ?: minLev)
                .coerceAtLeast(minLev)
            _currentLevel.value = savedLevel
            _targetLevel.value  = (savedLevel + 1).coerceAtMost(35)

            _dnaOnHand.value   = userDnaInventoryDao.get(profileId, dinoId)?.dnaAmount ?: 0
            _coinsOnHand.value = userWalletDao.get(profileId)?.coins ?: 0L
        }
    }

    fun setCurrentLevel(level: Int) {
        val unlockLevel = (_dinoData.value?.first?.rarity?.minLevel() ?: 1) - 1
        val clamped = level.coerceIn(unlockLevel, 34)
        _currentLevel.value = clamped
        if (_targetLevel.value <= clamped) {
            _targetLevel.value = (clamped + 1).coerceAtMost(35)
        }
    }

    fun setTargetLevel(level: Int) {
        _targetLevel.value = level.coerceIn(_currentLevel.value + 1, 35)
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
