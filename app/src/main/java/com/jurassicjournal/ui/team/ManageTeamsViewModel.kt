package com.jurassicjournal.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jurassicjournal.data.user.ActiveProfileRepository
import com.jurassicjournal.data.user.dao.TeamDao
import com.jurassicjournal.data.user.entity.Team
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageTeamsUiState(
    val teams: List<Team> = emptyList(),
    val profileId: Long = 1L,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ManageTeamsViewModel @Inject constructor(
    private val activeProfileRepository: ActiveProfileRepository,
    private val teamDao: TeamDao,
) : ViewModel() {

    val uiState: StateFlow<ManageTeamsUiState> = activeProfileRepository.activeProfileId
        .flatMapLatest { profileId ->
            teamDao.observeForProfile(profileId).combine(
                kotlinx.coroutines.flow.flowOf(profileId)
            ) { teams, id -> ManageTeamsUiState(teams, id) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageTeamsUiState())

    fun createTeam(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val profileId = uiState.value.profileId
            teamDao.insert(Team(profileId = profileId, name = trimmed))
        }
    }

    fun renameTeam(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            teamDao.getById(id)?.let { teamDao.update(it.copy(name = trimmed)) }
        }
    }

    fun deleteTeam(id: Long) {
        viewModelScope.launch { teamDao.deleteById(id) }
    }
}
