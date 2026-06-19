package com.jurassicjournal.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jurassicjournal.data.user.ActiveProfileRepository
import com.jurassicjournal.data.user.entity.Profile
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

@Serializable
private data class ProfileExport(val name: String, val sortOrder: Int)

data class ManageProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long = 1L,
    val errorMessage: String? = null,
)

@HiltViewModel
class ManageProfilesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activeProfileRepository: ActiveProfileRepository,
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

    fun exportProfile(profileId: Long, uri: Uri) {
        viewModelScope.launch {
            try {
                val profile = activeProfileRepository.profileDao.getById(profileId) ?: return@launch
                val export = ProfileExport(profile.name, profile.sortOrder)
                val json = Json.encodeToString(export)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            } catch (e: Exception) {
                _error.value = "Export failed: ${e.message}"
            }
        }
    }

    fun importProfile(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: return@launch
                val export = Json.decodeFromString<ProfileExport>(json)
                activeProfileRepository.createProfile(export.name)
            } catch (e: Exception) {
                _error.value = "Import failed: ${e.message}"
            }
        }
    }
}
