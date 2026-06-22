package com.jurassicjournal.ui.dino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jurassicjournal.data.game.repository.DinoRepository
import com.jurassicjournal.data.game.repository.DinoSearchResult
import com.jurassicjournal.data.model.DinoClass
import com.jurassicjournal.data.model.Rarity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilterState(
    val query: String = "",
    val rarity: Rarity? = null,
    val dinoClass: DinoClass? = null,
    val newOnly: Boolean = false,
)

@HiltViewModel
class DinoListViewModel @Inject constructor(
    private val repository: DinoRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(FilterState())
    val filters: StateFlow<FilterState> = _filters.asStateFlow()

    val newCount: StateFlow<Int> = repository.observeNewCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<DinoSearchResult>> = _filters
        .flatMapLatest { f ->
            repository.search(f.query, f.rarity, f.dinoClass).map { list ->
                if (f.newOnly) list.filter { it.isNew } else list
            }
        }
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
    fun onRarityFilter(rarity: Rarity?) = _filters.update { it.copy(rarity = rarity) }
    fun onClassFilter(dinoClass: DinoClass?) = _filters.update { it.copy(dinoClass = dinoClass) }
    fun onNewOnlyFilter(enabled: Boolean) = _filters.update { it.copy(newOnly = enabled) }
}
