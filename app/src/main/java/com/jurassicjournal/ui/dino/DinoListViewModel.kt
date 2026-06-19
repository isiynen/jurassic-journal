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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class FilterState(
    val query: String = "",
    val rarity: Rarity? = null,
    val dinoClass: DinoClass? = null,
)

@HiltViewModel
class DinoListViewModel @Inject constructor(
    private val repository: DinoRepository,
) : ViewModel() {

    private val _filters = MutableStateFlow(FilterState())
    val filters: StateFlow<FilterState> = _filters.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<DinoSearchResult>> = _filters
        .flatMapLatest { f ->
            repository.search(f.query, f.rarity, f.dinoClass)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(query: String) = _filters.update { it.copy(query = query) }
    fun onRarityFilter(rarity: Rarity?) = _filters.update { it.copy(rarity = rarity) }
    fun onClassFilter(dinoClass: DinoClass?) = _filters.update { it.copy(dinoClass = dinoClass) }
}
