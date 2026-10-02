package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoBaseStatDao
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoResistanceDao
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoSanctuaryPointDao
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoRepository
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoSearchResult
import com.sufficienteffort.jurassicjournal.data.model.BoostState
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.ResistanceType
import com.sufficienteffort.jurassicjournal.data.model.SpawnLocation
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.UserBoostDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Search + filter inputs shared by the dino list and the team dino picker. */
data class FilterState(
    val query: String = "",
    val rarities: Set<Rarity> = emptySet(),
    val dinoClasses: Set<DinoClass> = emptySet(),
    val newOnly: Boolean = false,
    val locations: Set<SpawnLocation> = emptySet(),
    val sortMode: StatSortMode? = null,
    val resistanceSort: ResistanceType? = null,
) {
    fun toggleRarity(r: Rarity) = copy(rarities = rarities.toggled(r))
    fun toggleClass(c: DinoClass) = copy(dinoClasses = dinoClasses.toggled(c))
    fun toggleLocation(l: SpawnLocation) = copy(locations = locations.toggled(l))

    private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DinoListViewModel @Inject constructor(
    private val repository: DinoRepository,
    private val dinoBaseStatDao: DinoBaseStatDao,
    private val dinoResistanceDao: DinoResistanceDao,
    private val dinoSanctuaryPointDao: DinoSanctuaryPointDao,
    private val userDinoDao: UserDinoDao,
    private val userBoostDao: UserBoostDao,
    private val activeProfileRepository: ActiveProfileRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(FilterState())
    val filters: StateFlow<FilterState> = _filters.asStateFlow()

    val newCount: StateFlow<Int> = repository.observeNewCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val results: StateFlow<List<DinoSearchResult>> = _filters
        .flatMapLatest { f ->
            repository.search(f.query, f.rarities, f.dinoClasses, f.locations).map { list ->
                if (f.newOnly) list.filter { it.isNew } else list
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val gameDataFlow = combine(
        dinoBaseStatDao.observeAll().map { list -> list.associateBy { it.dinoId } },
        dinoSanctuaryPointDao.observeAll().map { list -> list.associateBy { it.dinoId } },
        dinoResistanceDao.observeAll().map { list -> list.groupBy { it.dinoId } },
    ) { stats, sanctuary, resistances -> Triple(stats, sanctuary, resistances) }

    private val userDataFlow = activeProfileRepository.activeProfileId
        .flatMapLatest { profileId ->
            combine(
                userDinoDao.observeForProfile(profileId),
                userBoostDao.observeForProfile(profileId),
            ) { dinos, boosts ->
                val boostsByDino = boosts.groupBy { it.dinoId }
                val levelByDino = dinos.associate { it.dinoId to it.currentLevel }
                (levelByDino.keys + boostsByDino.keys).associateWith { id ->
                    DinoSorter.UserDinoData(
                        level = levelByDino[id],
                        boosts = BoostState.fromRows(boostsByDino[id] ?: emptyList()),
                    )
                }
            }
        }

    val listItems: StateFlow<List<DinoListItem>> = combine(
        results,
        _filters.map { it.sortMode to it.resistanceSort },
        gameDataFlow,
        userDataFlow,
    ) { filtered, (statSort, resistSort), (stats, sanctuary, resistances), userData ->
        DinoSorter.sort(filtered, statSort, resistSort, stats, sanctuary, userData, resistances)
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
    fun onRarityToggle(rarity: Rarity) = _filters.update { it.toggleRarity(rarity) }
    fun onRarityClear() = _filters.update { it.copy(rarities = emptySet()) }
    fun onClassToggle(dinoClass: DinoClass) = _filters.update { it.toggleClass(dinoClass) }
    fun onClassClear() = _filters.update { it.copy(dinoClasses = emptySet()) }
    fun onNewOnlyFilter(enabled: Boolean) = _filters.update { it.copy(newOnly = enabled) }
    fun onLocationToggle(location: SpawnLocation) = _filters.update { it.toggleLocation(location) }
    fun onLocationClear() = _filters.update { it.copy(locations = emptySet()) }
    fun onSortMode(mode: StatSortMode?) = _filters.update { it.copy(sortMode = mode, resistanceSort = null) }
    fun onResistanceSort(type: ResistanceType?) = _filters.update { it.copy(resistanceSort = type, sortMode = null) }
    fun resetFilters() = _filters.update { FilterState() }
}
