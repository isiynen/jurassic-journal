package com.jurassicjournal.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jurassicjournal.data.user.ActiveProfileRepository
import com.jurassicjournal.data.user.dao.OmegaTrainingAllocationDao
import com.jurassicjournal.data.user.dao.TeamDao
import com.jurassicjournal.data.user.dao.TeamMemberDao
import com.jurassicjournal.data.user.dao.UserBoostDao
import com.jurassicjournal.data.user.dao.UserDinoDao
import com.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.jurassicjournal.data.user.dao.UserWalletDao
import com.jurassicjournal.data.user.entity.OmegaTrainingAllocation
import com.jurassicjournal.data.user.entity.Profile
import com.jurassicjournal.data.user.entity.Team
import com.jurassicjournal.data.user.entity.TeamMember
import com.jurassicjournal.data.user.entity.UserBoost
import com.jurassicjournal.data.user.entity.UserDino
import com.jurassicjournal.data.user.entity.UserDnaInventory
import com.jurassicjournal.data.user.entity.UserWallet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

// ── Serialization model ───────────────────────────────────────────────────────

@Serializable
private data class ProfileExport(
    val name: String,
    val sortOrder: Int = 0,
    val userDinos: List<UserDinoExport> = emptyList(),
    val userBoosts: List<UserBoostExport> = emptyList(),
    val dnaInventory: List<UserDnaExport> = emptyList(),
    val omegaAllocations: List<OmegaAllocExport> = emptyList(),
    val wallet: WalletExport? = null,
    val teams: List<TeamExport> = emptyList(),
)

@Serializable
private data class UserDinoExport(
    val dinoId: Long,
    val isUnlocked: Boolean,
    val currentLevel: Int,
    val currentXp: Int,
)

@Serializable
private data class UserBoostExport(val dinoId: Long, val stat: String, val boostsApplied: Int)

@Serializable
private data class UserDnaExport(val dinoId: Long, val dnaAmount: Int)

@Serializable
private data class OmegaAllocExport(val dinoId: Long, val stat: String, val pointsAllocated: Int)

@Serializable
private data class WalletExport(val coins: Long, val hardCash: Int)

@Serializable
private data class TeamExport(val name: String, val sortOrder: Int, val members: List<TeamMemberExport>)

@Serializable
private data class TeamMemberExport(val dinoId: Long, val slotOrder: Int)

// ── UI state ──────────────────────────────────────────────────────────────────

data class ManageProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long = 1L,
    val errorMessage: String? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ManageProfilesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activeProfileRepository: ActiveProfileRepository,
    private val userDinoDao: UserDinoDao,
    private val userBoostDao: UserBoostDao,
    private val userDnaInventoryDao: UserDnaInventoryDao,
    private val omegaAllocationDao: OmegaTrainingAllocationDao,
    private val userWalletDao: UserWalletDao,
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ManageProfilesUiState> = combine(
        activeProfileRepository.observeProfiles(),
        activeProfileRepository.activeProfileId,
        _error,
    ) { profiles, activeId, err ->
        ManageProfilesUiState(profiles, activeId, err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageProfilesUiState())

    fun createProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { activeProfileRepository.createProfile(trimmed) }
    }

    fun renameProfile(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { activeProfileRepository.renameProfile(id, trimmed) }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            val current = uiState.value
            if (current.profiles.size <= 1) {
                _error.value = "Cannot delete the last profile."
                return@launch
            }
            activeProfileRepository.deleteProfile(id)
            if (current.activeProfileId == id) {
                val remaining = current.profiles.firstOrNull { it.id != id }
                if (remaining != null) activeProfileRepository.setActiveProfile(remaining.id)
            }
        }
    }

    fun clearError() { _error.value = null }

    // ── Export: full profile snapshot including all user data ─────────────────

    fun exportProfile(profileId: Long, uri: Uri) {
        viewModelScope.launch {
            try {
                val profile = activeProfileRepository.profileDao.getById(profileId) ?: return@launch

                val dinos = userDinoDao.getForProfile(profileId)
                val boosts = userBoostDao.getForProfile(profileId)
                val dna = userDnaInventoryDao.getForProfile(profileId)
                val omega = omegaAllocationDao.getForProfile(profileId)
                val wallet = userWalletDao.get(profileId)
                val teams = teamDao.getForProfile(profileId)
                val allMembers = teamMemberDao.getForProfile(profileId).groupBy { it.teamId }

                val export = ProfileExport(
                    name = profile.name,
                    sortOrder = profile.sortOrder,
                    userDinos = dinos.map { UserDinoExport(it.dinoId, it.isUnlocked, it.currentLevel, it.currentXp) },
                    userBoosts = boosts.map { UserBoostExport(it.dinoId, it.stat, it.boostsApplied) },
                    dnaInventory = dna.map { UserDnaExport(it.dinoId, it.dnaAmount) },
                    omegaAllocations = omega.map { OmegaAllocExport(it.dinoId, it.stat, it.pointsAllocated) },
                    wallet = wallet?.let { WalletExport(it.coins, it.hardCash) },
                    teams = teams.map { team ->
                        TeamExport(
                            name = team.name,
                            sortOrder = team.sortOrder,
                            members = (allMembers[team.id] ?: emptyList()).map {
                                TeamMemberExport(it.dinoId, it.slotOrder)
                            },
                        )
                    },
                )

                val json = Json.encodeToString(export)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            } catch (e: Exception) {
                _error.value = "Export failed: ${e.message}"
            }
        }
    }

    // ── Import: restore full profile snapshot ─────────────────────────────────

    fun importProfile(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: return@launch

                val export = Json { ignoreUnknownKeys = true }.decodeFromString<ProfileExport>(json)
                val newProfileId = activeProfileRepository.createProfile(export.name)

                // Restore user data under the new profileId
                if (export.userDinos.isNotEmpty()) {
                    userDinoDao.insertAll(export.userDinos.map {
                        UserDino(newProfileId, it.dinoId, it.isUnlocked, it.currentLevel, it.currentXp)
                    })
                }
                if (export.userBoosts.isNotEmpty()) {
                    userBoostDao.insertAll(export.userBoosts.map {
                        UserBoost(newProfileId, it.dinoId, it.stat, it.boostsApplied)
                    })
                }
                if (export.dnaInventory.isNotEmpty()) {
                    userDnaInventoryDao.insertAll(export.dnaInventory.map {
                        UserDnaInventory(newProfileId, it.dinoId, it.dnaAmount)
                    })
                }
                if (export.omegaAllocations.isNotEmpty()) {
                    omegaAllocationDao.insertAll(export.omegaAllocations.map {
                        OmegaTrainingAllocation(newProfileId, it.dinoId, it.stat, it.pointsAllocated)
                    })
                }
                export.wallet?.let {
                    userWalletDao.upsert(UserWallet(newProfileId, it.coins, it.hardCash))
                }
                export.teams.forEach { teamExport ->
                    val newTeamId = teamDao.insert(Team(profileId = newProfileId, name = teamExport.name, sortOrder = teamExport.sortOrder))
                    if (teamExport.members.isNotEmpty()) {
                        teamMemberDao.insertAll(teamExport.members.map {
                            TeamMember(newTeamId, it.dinoId, it.slotOrder)
                        })
                    }
                }
            } catch (e: Exception) {
                _error.value = "Import failed: ${e.message}"
            }
        }
    }
}
