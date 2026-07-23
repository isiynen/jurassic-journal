package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoBaseStatDao
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoResistanceDao
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoRepository
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoSearchResult
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.ProgressionSystem
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.ResistanceType
import com.sufficienteffort.jurassicjournal.data.model.SpawnLocation
import com.sufficienteffort.jurassicjournal.data.model.displayName
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.UserBoostDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoDao
import com.sufficienteffort.jurassicjournal.util.StatCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatSortMode { DAMAGE, HEALTH, SPEED, ARMOR, CRIT }

sealed class DinoListItem {
    data class Header(val label: String) : DinoListItem()
    data class Item(val result: DinoSearchResult) : DinoListItem()
}

data class FilterState(
    val query: String = "",
    val rarities: Set<Rarity> = emptySet(),
    val dinoClasses: Set<DinoClass> = emptySet(),
    val newOnly: Boolean = false,
    val locations: Set<SpawnLocation> = emptySet(),
    val sortMode: StatSortMode? = null,
    val resistanceSort: ResistanceType? = null,
)

@HiltViewModel
class DinoListViewModel @Inject constructor(
    private val repository: DinoRepository,
    private val dinoBaseStatDao: DinoBaseStatDao,
    private val dinoResistanceDao: DinoResistanceDao,
    private val userDinoDao: UserDinoDao,
    private val userBoostDao: UserBoostDao,
    private val activeProfileRepository: ActiveProfileRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(FilterState())
    val filters: StateFlow<FilterState> = _filters.asStateFlow()

    val newCount: StateFlow<Int> = repository.observeNewCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val results: StateFlow<List<DinoSearchResult>> = _filters
        .flatMapLatest { f ->
            repository.search(f.query, f.rarities, f.dinoClasses, f.locations).map { list ->
                if (f.newOnly) list.filter { it.isNew } else list
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allStatsFlow = dinoBaseStatDao.observeAll()
        .map { list -> list.associateBy { it.dinoId } }

    private val allResistancesFlow = dinoResistanceDao.observeAll()
        .map { list -> list.groupBy { it.dinoId } }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val userDataFlow = activeProfileRepository.activeProfileId
        .flatMapLatest { profileId ->
            combine(
                userDinoDao.observeForProfile(profileId),
                userBoostDao.observeForProfile(profileId),
            ) { dinos, boosts ->
                val levelsMap = dinos.associateBy { it.dinoId }
                val boostsByDinoId = boosts.groupBy { it.dinoId }
                levelsMap to boostsByDinoId
            }
        }

    val listItems: StateFlow<List<DinoListItem>> = combine(
        results,
        _filters.map { it.sortMode to it.resistanceSort },
        allStatsFlow,
        userDataFlow,
        allResistancesFlow,
    ) { filtered, sortPair, statsMap, userData, resistancesMap ->
        val statSort = sortPair.first
        val resistSort = sortPair.second
        val (userLevelsMap, boostsByDinoId) = userData
        when {
            statSort != null -> {
                val withStat = filtered.map { result ->
                    val baseStat = statsMap[result.dino.id]
                    val isOmega = result.dino.progressionSystem == ProgressionSystem.TRAINING_POINT
                    val level = if (isOmega) 26 else userLevelsMap[result.dino.id]?.currentLevel ?: 26
                    val dinoBoosts = boostsByDinoId[result.dino.id] ?: emptyList()
                    val stat = if (baseStat == null) 0 else when (statSort) {
                        StatSortMode.DAMAGE -> {
                            val scaled = if (isOmega) baseStat.baseAttack
                                         else StatCalculator.scaleStat(baseStat.baseAttack, level)
                            val tiers = dinoBoosts.firstOrNull { it.stat == "attack" }?.boostsApplied ?: 0
                            StatCalculator.applyAttackBoost(scaled, tiers)
                        }
                        StatSortMode.HEALTH -> {
                            val scaled = if (isOmega) baseStat.baseHealth
                                         else StatCalculator.scaleStat(baseStat.baseHealth, level)
                            val tiers = dinoBoosts.firstOrNull { it.stat == "health" }?.boostsApplied ?: 0
                            StatCalculator.applyHealthBoost(scaled, tiers)
                        }
                        StatSortMode.SPEED -> {
                            val tiers = dinoBoosts.firstOrNull { it.stat == "speed" }?.boostsApplied ?: 0
                            StatCalculator.applySpeedBoost(baseStat.speed, tiers)
                        }
                        StatSortMode.ARMOR -> baseStat.armor.toInt()
                        StatSortMode.CRIT -> baseStat.critChance.toInt()
                    }
                    result to stat
                }
                buildGroupedList(withStat.sortedByDescending { it.second }, statSort)
            }
            resistSort != null -> {
                val withResist = filtered.map { result ->
                    val pct = resistancesMap[result.dino.id]
                        ?.firstOrNull { it.resistType == resistSort }
                        ?.percentage ?: 0
                    result to pct
                }
                buildResistanceGroupedList(withResist.sortedByDescending { it.second }, resistSort)
            }
            else -> filtered.map { DinoListItem.Item(it) }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Auto-clear the newOnly filter when no new dinos remain for this profile
        viewModelScope.launch {
            newCount.collect { count ->
                if (count == 0 && _filters.value.newOnly) {
                    _filters.update { it.copy(newOnly = false) }
                }
            }
        }
    }

    fun onQueryChange(query: String) = _filters.update { it.copy(query = query) }
    fun onRarityToggle(rarity: Rarity) = _filters.update { f ->
        val updated = if (rarity in f.rarities) f.rarities - rarity else f.rarities + rarity
        f.copy(rarities = updated)
    }
    fun onRarityClear() = _filters.update { it.copy(rarities = emptySet()) }
    fun onClassToggle(dinoClass: DinoClass) = _filters.update { f ->
        val updated = if (dinoClass in f.dinoClasses) f.dinoClasses - dinoClass else f.dinoClasses + dinoClass
        f.copy(dinoClasses = updated)
    }
    fun onClassClear() = _filters.update { it.copy(dinoClasses = emptySet()) }
    fun onNewOnlyFilter(enabled: Boolean) = _filters.update { it.copy(newOnly = enabled) }
    fun onLocationToggle(location: SpawnLocation) = _filters.update { f ->
        val updated = if (location in f.locations) f.locations - location else f.locations + location
        f.copy(locations = updated)
    }
    fun onLocationClear() = _filters.update { it.copy(locations = emptySet()) }
    fun onSortMode(mode: StatSortMode?) = _filters.update { it.copy(sortMode = mode, resistanceSort = null) }
    fun onResistanceSort(type: ResistanceType?) = _filters.update { it.copy(resistanceSort = type, sortMode = null) }
    fun resetFilters() = _filters.update { FilterState() }

    private fun buildGroupedList(
        sorted: List<Pair<DinoSearchResult, Int>>,
        sortMode: StatSortMode,
    ): List<DinoListItem> {
        if (sortMode == StatSortMode.SPEED) {
            val items = mutableListOf<DinoListItem>()
            var currentSpeed = Int.MIN_VALUE
            for ((result, stat) in sorted) {
                if (stat != currentSpeed) {
                    currentSpeed = stat
                    items.add(DinoListItem.Header("Speed $stat"))
                }
                items.add(DinoListItem.Item(result))
            }
            return items
        }
        val bucketSize = when (sortMode) {
            StatSortMode.DAMAGE -> 200
            StatSortMode.HEALTH -> 500
            StatSortMode.ARMOR, StatSortMode.CRIT -> 5
            StatSortMode.SPEED -> error("unreachable")
        }
        val items = mutableListOf<DinoListItem>()
        var currentBucket = Int.MIN_VALUE
        for ((result, stat) in sorted) {
            val bucket = (stat / bucketSize) * bucketSize
            if (bucket != currentBucket) {
                currentBucket = bucket
                val lo = if (bucket == 0) 1 else bucket
                val hi = bucket + bucketSize - 1
                val label = when (sortMode) {
                    StatSortMode.DAMAGE -> "Damage $lo–$hi"
                    StatSortMode.HEALTH -> "Health $lo–$hi"
                    StatSortMode.ARMOR -> "Armor $bucket%"
                    StatSortMode.CRIT -> "Crit $bucket%"
                    StatSortMode.SPEED -> error("unreachable")
                }
                items.add(DinoListItem.Header(label))
            }
            items.add(DinoListItem.Item(result))
        }
        return items
    }

    private fun buildResistanceGroupedList(
        sorted: List<Pair<DinoSearchResult, Int>>,
        resistType: ResistanceType,
    ): List<DinoListItem> {
        val items = mutableListOf<DinoListItem>()
        var currentBucket = Int.MIN_VALUE
        for ((result, pct) in sorted) {
            val bucket = (pct / 5) * 5
            if (bucket != currentBucket) {
                currentBucket = bucket
                items.add(DinoListItem.Header("${resistType.displayName()} $bucket%"))
            }
            items.add(DinoListItem.Item(result))
        }
        return items
    }
}
