package com.sufficienteffort.jurassicjournal.ui.ability

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sufficienteffort.jurassicjournal.data.game.repository.AbilityDetail
import com.sufficienteffort.jurassicjournal.data.game.repository.AbilityRepository
import com.sufficienteffort.jurassicjournal.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AbilityDetailUiState(
    val isLoading: Boolean = true,
    val detail: AbilityDetail? = null,
    val threatenedFirst: Boolean = false,
)

@HiltViewModel
class AbilityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AbilityRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AbilityDetailUiState())
    val state: StateFlow<AbilityDetailUiState> = _state.asStateFlow()

    init {
        val route = savedStateHandle.toRoute<Screen.AbilityDetail>()
        viewModelScope.launch {
            _state.value = AbilityDetailUiState(
                isLoading = false,
                detail = repository.getDetail(route.slug),
                threatenedFirst = route.threatened,
            )
        }
    }
}
