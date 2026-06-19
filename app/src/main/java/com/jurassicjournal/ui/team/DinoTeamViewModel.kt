package com.jurassicjournal.ui.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jurassicjournal.data.user.ActiveProfileRepository
import com.jurassicjournal.data.user.dao.TeamDao
import com.jurassicjournal.data.user.dao.TeamMemberDao
import com.jurassicjournal.data.user.entity.Team
import com.jurassicjournal.data.user.entity.TeamMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DinoTeamState(
    val availableTeams: List<Team> = emptyList(),
    val memberTeamIds: Set<Long> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DinoTeamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activeProfileRepository: ActiveProfileRepository,
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
) : ViewModel() {

    private val dinoId: Long = checkNotNull(savedStateHandle["dinoId"])

    val state: StateFlow<DinoTeamState> = activeProfileRepository.activeProfileId
        .flatMapLatest { profileId ->
            combine(
                teamDao.observeForProfile(profileId),
                teamMemberDao.observeTeamIdsForDino(dinoId),
            ) { teams, memberIds ->
                DinoTeamState(teams, memberIds.toSet())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DinoTeamState())

    fun addToTeam(teamId: Long) {
        viewModelScope.launch {
            val existing = teamMemberDao.getForTeam(teamId)
            val nextSlot = (existing.maxOfOrNull { it.slotOrder } ?: -1) + 1
            teamMemberDao.insert(TeamMember(teamId = teamId, dinoId = dinoId, slotOrder = nextSlot))
        }
    }

    fun removeFromTeam(teamId: Long) {
        viewModelScope.launch { teamMemberDao.remove(teamId, dinoId) }
    }
}
