package com.sufficienteffort.jurassicjournal.ui.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoRepository
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoSearchResult
import com.sufficienteffort.jurassicjournal.data.user.dao.TeamDao
import com.sufficienteffort.jurassicjournal.data.user.dao.TeamMemberDao
import com.sufficienteffort.jurassicjournal.data.user.entity.TeamMember
import com.sufficienteffort.jurassicjournal.ui.dino.FilterState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TeamDinoPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
    private val repository: DinoRepository,
) : ViewModel() {

    private val teamId: Long = checkNotNull(savedStateHandle["teamId"])

    private val _teamName = MutableStateFlow("")
    val teamName: StateFlow<String> = _teamName.asStateFlow()

    private val _filters = MutableStateFlow(FilterState())
    val filters: StateFlow<FilterState> = _filters.asStateFlow()

    private val _initialIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _stagedIds = MutableStateFlow<Set<Long>>(emptySet())
    val stagedIds: StateFlow<Set<Long>> = _stagedIds.asStateFlow()

    val hasChanges: StateFlow<Boolean> = combine(_stagedIds, _initialIds) { staged, initial ->
        staged != initial
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val newCount: StateFlow<Int> = repository.observeNewCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val results: StateFlow<List<DinoSearchResult>> = _filters
        .flatMapLatest { f ->
            repository.search(f.query, f.rarities, f.dinoClasses).map { list ->
                if (f.newOnly) list.filter { it.isNew } else list
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saveCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saveCompleted: SharedFlow<Unit> = _saveCompleted.asSharedFlow()

    init {
        viewModelScope.launch {
            _teamName.value = teamDao.getById(teamId)?.name ?: ""
            val members = teamMemberDao.getForTeam(teamId)
            val ids = members.map { it.dinoId }.toSet()
            _initialIds.value = ids
            _stagedIds.value = ids
        }
        viewModelScope.launch {
            newCount.collect { count ->
                if (count == 0 && _filters.value.newOnly) {
                    _filters.update { it.copy(newOnly = false) }
                }
            }
        }
    }

    fun toggle(dinoId: Long) {
        _stagedIds.update { if (dinoId in it) it - dinoId else it + dinoId }
    }

    fun onQueryChange(query: String) = _filters.update { it.copy(query = query) }
    fun onRarityToggle(rarity: com.sufficienteffort.jurassicjournal.data.model.Rarity) = _filters.update { f ->
        val updated = if (rarity in f.rarities) f.rarities - rarity else f.rarities + rarity
        f.copy(rarities = updated)
    }
    fun onRarityClear() = _filters.update { it.copy(rarities = emptySet()) }
    fun onClassToggle(dinoClass: com.sufficienteffort.jurassicjournal.data.model.DinoClass) = _filters.update { f ->
        val updated = if (dinoClass in f.dinoClasses) f.dinoClasses - dinoClass else f.dinoClasses + dinoClass
        f.copy(dinoClasses = updated)
    }
    fun onClassClear() = _filters.update { it.copy(dinoClasses = emptySet()) }
    fun onNewOnlyFilter(enabled: Boolean) = _filters.update { it.copy(newOnly = enabled) }

    fun save() {
        viewModelScope.launch {
            val initial = _initialIds.value
            val staged = _stagedIds.value
            val toRemove = initial - staged
            val toAdd = staged - initial

            for (id in toRemove) teamMemberDao.remove(teamId, id)

            if (toAdd.isNotEmpty()) {
                val existing = teamMemberDao.getForTeam(teamId)
                var nextSlot = (existing.maxOfOrNull { it.slotOrder } ?: -1) + 1
                for (id in toAdd) {
                    teamMemberDao.insert(TeamMember(teamId = teamId, dinoId = id, slotOrder = nextSlot++))
                }
            }

            _initialIds.value = staged
            _saveCompleted.emit(Unit)
        }
    }
}
