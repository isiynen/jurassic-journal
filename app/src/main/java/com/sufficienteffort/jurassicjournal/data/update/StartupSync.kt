package com.sufficienteffort.jurassicjournal.data.update

import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs the launch-time housekeeping and asset syncs once per process, on an
 * application-lifetime scope so rotating or recreating the activity mid-sync
 * does not cancel it.
 *
 * Sequential on purpose — the image and icon syncs share one SyncProgressTracker,
 * and running them concurrently lets each clobber the other's phase and reset
 * the progress strip mid-sync.
 */
@Singleton
class StartupSync @Inject constructor(
    private val activeProfileRepository: ActiveProfileRepository,
    private val newDinoDetector: NewDinoDetector,
    private val dinoImageSync: DinoImageSync,
    private val abilityIconSync: AbilityIconSync,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        scope.launch {
            activeProfileRepository.pruneOrphanedData()
            newDinoDetector.detect()
            dinoImageSync.syncMissingImages()
            abilityIconSync.syncMissingIcons()
        }
    }
}
