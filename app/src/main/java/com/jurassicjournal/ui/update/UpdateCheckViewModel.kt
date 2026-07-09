package com.sufficienteffort.jurassicjournal.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sufficienteffort.jurassicjournal.data.update.GameDataUpdater
import com.sufficienteffort.jurassicjournal.data.update.SyncProgressTracker
import com.sufficienteffort.jurassicjournal.data.update.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpdateState {
    object Idle         : UpdateState()
    object Checking     : UpdateState()
    object Downloading  : UpdateState()
    object RestartReady : UpdateState()
}

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progressTracker: SyncProgressTracker,
) : ViewModel() {

    private val updater = GameDataUpdater(context)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Checking)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val info = updater.checkForUpdate()
            _updateInfo.value = info
            _state.value = UpdateState.Idle
        }
    }

    fun confirmUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = UpdateState.Downloading
            try {
                updater.downloadAndStage(info, progressTracker)
                _state.value = UpdateState.RestartReady
            } catch (e: Exception) {
                android.util.Log.e("UpdateCheckViewModel", "Download failed: ${e::class.simpleName}: ${e.message}")
                // Clear so the dialog goes away; will retry next launch
                _updateInfo.value = null
                _state.value = UpdateState.Idle
            }
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
        _state.value = UpdateState.Idle
    }
}
