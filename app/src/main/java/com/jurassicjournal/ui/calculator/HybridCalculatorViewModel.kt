package com.sufficienteffort.jurassicjournal.ui.calculator

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sufficienteffort.jurassicjournal.data.game.dao.LevelUpCostDao
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.game.entity.LevelUpCost
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoDetailRepository
import com.sufficienteffort.jurassicjournal.data.game.repository.IngredientNode
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserWalletDao
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDnaInventory
import com.sufficienteffort.jurassicjournal.data.user.entity.UserWallet
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
    val depth: Int = 0,
    val parentDinoId: Long? = null,
    val parentDinoName: String? = null,
    val dnaOnHand: Int = 0,
)

data class IngredientCost(
    val dino: Dino,
    val depth: Int = 0,
    val parentDinoId: Long? = null,
    val parentDinoName: String? = null,
    val dnaCostPerFuse: Int,
    val fusesOfParent: Int,
    val totalDnaNeeded: Long,
    val dnaOnHand: Int,
    val dnaDeficit: Long,
    val fusesNeededToProduce: Int = 0,
    val fuseCoinCost: Long = 0L,
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

    // Tree structure used for recursive cost calculations; set once in init.
    private var ingredientTree: List<IngredientNode> = emptyList()

    val uiState: StateFlow<HybridCalculatorUiState> = combine(
        combine(_hybridData, _isCreate, _currentLevel) { hd, ic, cl -> Triple(hd, ic, cl) },
        combine(_targetLevel, _currentHybridDna, _ingredients) { tl, cd, ing -> Triple(tl, cd, ing) },
        _coinsOnHand,
    ) { (hybridData, isCreate, currentLevel), (targetLevel, currentHybridDna, ingredients), coinsOnHand ->
        val (hybrid, costs) = hybridData ?: return@combine HybridCalculatorUiState(isLoading = true)

        val result = if (targetLevel >= currentLevel) {
            calculateCosts(isCreate, hybrid.rarity, currentLevel, targetLevel, currentHybridDna, ingredients, ingredientTree, costs, coinsOnHand)
        } else null

        val maxReachableLevel = calculateMaxReachableLevel(
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
            profileId = activeProfileRepository.activeProfileId.first()

            val detail = detailRepository.getFullDetail(dinoId) ?: return@launch
            val costs  = levelUpCostDao.getForRarity(detail.dino.rarity)

            ingredientTree = detail.ingredientTree
            _hybridData.value = detail.dino to costs

            val minLev = detail.dino.rarity.minLevel()
            val savedLevel = (userDinoDao.getByDinoId(profileId, dinoId)?.currentLevel ?: minLev)
                .coerceAtLeast(minLev)
            _currentLevel.value = savedLevel
            _targetLevel.value  = (savedLevel + 1).coerceAtMost(35)

            _currentHybridDna.value = userDnaInventoryDao.get(profileId, dinoId)?.dnaAmount ?: 0

            // Flatten full ingredient tree in DFS order, then stable-sort by depth so
            // all depth-0 nodes precede depth-1 nodes, etc. (BFS display order).
            val flatDfs = mutableListOf<IngredientInput>()
            flattenIngredientTree(detail.ingredientTree, 0, null, null, flatDfs)
            val flatBfs = flatDfs.sortedBy { it.depth }

            val allDinoIds = flatBfs.map { it.dino.id }
            val savedDnaMap = userDnaInventoryDao.getForDinos(profileId, allDinoIds).associateBy { it.dinoId }
            _ingredients.value = flatBfs.map { node ->
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

    // ── Tree helpers ──────────────────────────────────────────────────────────

    private fun flattenIngredientTree(
        nodes: List<IngredientNode>,
        depth: Int,
        parentDinoId: Long?,
        parentDinoName: String?,
        result: MutableList<IngredientInput>,
    ) {
        for (node in nodes) {
            result.add(IngredientInput(
                dino           = node.dino,
                depth          = depth,
                parentDinoId   = parentDinoId,
                parentDinoName = parentDinoName,
            ))
            flattenIngredientTree(node.children, depth + 1, node.dino.id, node.dino.name, result)
        }
    }

    // Recursively build the flat ingredient cost list (DFS preorder) and return total
    // coin cost for all sub-hybrid fuses required to produce the needed DNA.
    private fun buildIngredientCosts(
        tree: List<IngredientNode>,
        fusesOfParent: Int,
        dnaMap: Map<Long, Int>,
        depth: Int,
        parentDinoId: Long?,
        parentDinoName: String?,
        result: MutableList<IngredientCost>,
    ): Long {
        var totalSubFuseCoins = 0L
        for (node in tree) {
            val costPerFuse    = fuseCostForRarity(node.dino.rarity)
            val totalDnaNeeded = fusesOfParent.toLong() * costPerFuse
            val dnaOnHand      = dnaMap[node.dino.id] ?: 0
            val dnaDeficit     = maxOf(0L, totalDnaNeeded - dnaOnHand)
            val fusesNeededToProduce = if (node.dino.isHybrid && dnaDeficit > 0L)
                ceil(dnaDeficit / 20.0).toInt()
            else 0
            val fuseCoinCost = fusesNeededToProduce.toLong() * fuseCoinCostForRarity(node.dino.rarity)
            totalSubFuseCoins += fuseCoinCost
            result.add(IngredientCost(
                dino                 = node.dino,
                depth                = depth,
                parentDinoId         = parentDinoId,
                parentDinoName       = parentDinoName,
                dnaCostPerFuse       = costPerFuse,
                fusesOfParent        = fusesOfParent,
                totalDnaNeeded       = totalDnaNeeded,
                dnaOnHand            = dnaOnHand,
                dnaDeficit           = dnaDeficit,
                fusesNeededToProduce = fusesNeededToProduce,
                fuseCoinCost         = fuseCoinCost,
            ))
            if (node.children.isNotEmpty()) {
                totalSubFuseCoins += buildIngredientCosts(
                    tree           = node.children,
                    fusesOfParent  = fusesNeededToProduce,
                    dnaMap         = dnaMap,
                    depth          = depth + 1,
                    parentDinoId   = node.dino.id,
                    parentDinoName = node.dino.name,
                    result         = result,
                )
            }
        }
        return totalSubFuseCoins
    }

    // Returns total coin cost for all sub-hybrid fuses, or null if a non-hybrid ingredient
    // has insufficient DNA (impossible to produce the needed amount).
    private fun calcSubFuseCoins(
        tree: List<IngredientNode>,
        fusesNeeded: Int,
        dnaAvail: Map<Long, Long>,
    ): Long? {
        if (fusesNeeded == 0) return 0L
        var totalCoins = 0L
        for (node in tree) {
            val needed  = fusesNeeded.toLong() * fuseCostForRarity(node.dino.rarity)
            val have    = dnaAvail.getOrDefault(node.dino.id, 0L)
            val deficit = maxOf(0L, needed - have)
            if (deficit == 0L) continue
            if (!node.dino.isHybrid) return null
            val subFuses = ceil(deficit / 20.0).toInt()
            totalCoins += subFuses.toLong() * fuseCoinCostForRarity(node.dino.rarity)
            totalCoins += calcSubFuseCoins(node.children, subFuses, dnaAvail) ?: return null
        }
        return totalCoins
    }

    // Mutates dnaAvail to simulate spending ingredient DNA (and producing sub-hybrid DNA
    // as needed). Only call after calcSubFuseCoins confirms affordability.
    private fun spendIngredientDna(
        tree: List<IngredientNode>,
        fusesNeeded: Int,
        dnaAvail: MutableMap<Long, Long>,
    ) {
        if (fusesNeeded == 0) return
        for (node in tree) {
            val needed  = fusesNeeded.toLong() * fuseCostForRarity(node.dino.rarity)
            val have    = dnaAvail.getOrDefault(node.dino.id, 0L)
            val deficit = maxOf(0L, needed - have)
            if (deficit > 0L && node.dino.isHybrid) {
                val subFuses = ceil(deficit / 20.0).toInt()
                spendIngredientDna(node.children, subFuses, dnaAvail)
                dnaAvail[node.dino.id] = have + subFuses * 20L
            }
            dnaAvail[node.dino.id] = (dnaAvail.getOrDefault(node.dino.id, 0L)) - needed
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
        ingredientTree: List<IngredientNode>,
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

        val dnaMap = ingredients.associate { it.dino.id to it.dnaOnHand }

        val ingredientCostsDfs = mutableListOf<IngredientCost>()
        val subFuseCoins = buildIngredientCosts(
            tree           = ingredientTree,
            fusesOfParent  = fusesNeeded,
            dnaMap         = dnaMap,
            depth          = 0,
            parentDinoId   = null,
            parentDinoName = null,
            result         = ingredientCostsDfs,
        )
        // Stable-sort by depth so depth-0 costs precede depth-1 costs, etc.
        val ingredientCosts = ingredientCostsDfs.sortedBy { it.depth }

        val totalCoins = levelUpCoins + fuseCoins + subFuseCoins

        return CalcResult(
            hybridDnaStillNeeded = remainingHybridDna,
            fusesNeeded          = fusesNeeded,
            coinsNeeded          = totalCoins,
            coinDeficit          = maxOf(0L, totalCoins - coinsOnHand),
            ingredientCosts      = ingredientCosts,
        )
    }

    private fun calculateMaxReachableLevel(
        isCreate: Boolean,
        rarity: Rarity,
        currentLevel: Int,
        currentHybridDna: Int,
        ingredients: List<IngredientInput>,
        ingredientTree: List<IngredientNode>,
        coinsOnHand: Long,
        costs: List<LevelUpCost>,
    ): Int {
        val costMap = costs.associateBy { it.fromLevel }
        var hybridDnaAvail = currentHybridDna.toLong()
        val dnaAvail       = ingredients.associate { it.dino.id to it.dnaOnHand.toLong() }.toMutableMap()
        var coinsAvail     = coinsOnHand
        var maxLevel       = currentLevel

        if (isCreate) {
            val creationDnaNeeded = creationDnaCostForRarity(rarity).toLong()
            val deficit           = maxOf(0L, creationDnaNeeded - hybridDnaAvail)
            val fusesNeeded       = if (deficit > 0L) ceil(deficit / 20.0).toInt() else 0
            val fuseCoinsNeeded   = fusesNeeded.toLong() * fuseCoinCostForRarity(rarity)
            val subCoinCost       = calcSubFuseCoins(ingredientTree, fusesNeeded, dnaAvail) ?: return currentLevel - 1
            if (coinsAvail < fuseCoinsNeeded + subCoinCost) return currentLevel - 1
            coinsAvail -= fuseCoinsNeeded + subCoinCost
            spendIngredientDna(ingredientTree, fusesNeeded, dnaAvail)
            hybridDnaAvail = hybridDnaAvail + fusesNeeded * 20L - creationDnaNeeded
        }

        for (fromLevel in currentLevel until 35) {
            val cost = costMap[fromLevel] ?: break

            val hybridDnaNeeded  = cost.dnaCost.toLong()
            val deficit          = maxOf(0L, hybridDnaNeeded - hybridDnaAvail)
            val fusesNeeded      = if (deficit > 0L) ceil(deficit / 20.0).toInt() else 0
            val fuseCoinsNeeded  = fusesNeeded.toLong() * fuseCoinCostForRarity(rarity)
            val subCoinCost      = calcSubFuseCoins(ingredientTree, fusesNeeded, dnaAvail) ?: break
            val totalCoinsNeeded = cost.coinsCost + fuseCoinsNeeded + subCoinCost
            if (coinsAvail < totalCoinsNeeded) break

            coinsAvail -= totalCoinsNeeded
            spendIngredientDna(ingredientTree, fusesNeeded, dnaAvail)
            // Leftover hybrid DNA carries forward.
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
