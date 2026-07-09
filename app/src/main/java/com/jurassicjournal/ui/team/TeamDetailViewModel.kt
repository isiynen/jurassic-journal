package com.sufficienteffort.jurassicjournal.ui.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoRepository
import com.sufficienteffort.jurassicjournal.data.user.dao.TeamDao
import com.sufficienteffort.jurassicjournal.data.user.dao.TeamMemberDao
import com.sufficienteffort.jurassicjournal.data.user.entity.Team
import com.sufficienteffort.jurassicjournal.data.user.entity.TeamMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamDetailUiState(
    val team: Team? = null,
    val members: List<Dino> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TeamDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
    private val dinoRepository: DinoRepository,
) : ViewModel() {

    private val teamId: Long = checkNotNull(savedStateHandle["teamId"])

    private val _team = MutableStateFlow<Team?>(null)

    val uiState: StateFlow<TeamDetailUiState> = combine(
        _team,
        teamMemberDao.observeForTeam(teamId),
    ) { team, members ->
        if (team == null) return@combine TeamDetailUiState()
        val dinoIds = members.map { it.dinoId }
        val dinos = dinoRepository.getDinosByIds(dinoIds)
        val ordered = dinoIds.mapNotNull { id -> dinos.firstOrNull { it.id == id } }
        TeamDetailUiState(team, ordered, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamDetailUiState())

    init {
        viewModelScope.launch {
            _team.value = teamDao.getById(teamId)
        }
    }

    fun addDino(dinoId: Long) {
        viewModelScope.launch {
            val existing = teamMemberDao.getForTeam(teamId)
            val nextSlot = (existing.maxOfOrNull { it.slotOrder } ?: -1) + 1
            teamMemberDao.insert(TeamMember(teamId = teamId, dinoId = dinoId, slotOrder = nextSlot))
        }
    }

    fun removeDino(dinoId: Long) {
        viewModelScope.launch { teamMemberDao.remove(teamId, dinoId) }
    }
}
