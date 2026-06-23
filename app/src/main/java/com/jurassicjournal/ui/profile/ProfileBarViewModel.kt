package com.jurassicjournal.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jurassicjournal.data.user.ActiveProfileRepository
import com.jurassicjournal.data.user.dao.TeamDao
import com.jurassicjournal.data.user.entity.Profile
import com.jurassicjournal.data.user.entity.Team
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileBarState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long = 1L,
    val teams: List<Team> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileBarViewModel @Inject constructor(
    private val activeProfileRepository: ActiveProfileRepository,
    private val teamDao: TeamDao,
) : ViewModel() {

    val state: StateFlow<ProfileBarState> = combine(
        activeProfileRepository.observeProfiles(),
        activeProfileRepository.activeProfileId,
    ) { profiles, activeId -> profiles to activeId }
        .flatMapLatest { (profiles, activeId) ->
            teamDao.observeForProfile(activeId).map { teams ->
                ProfileBarState(profiles, activeId, teams)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileBarState())

    fun setActiveProfile(id: Long) {
        viewModelScope.launch { activeProfileRepository.setActiveProfile(id) }
    }
}
