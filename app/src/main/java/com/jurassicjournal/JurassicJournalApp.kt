package com.jurassicjournal

import android.app.Application
import android.util.Log
import coil.EventListener
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.jurassicjournal.data.update.BundledAbilityIcons
import com.jurassicjournal.data.update.BundledDinoImages
import com.jurassicjournal.data.update.GameDataUpdater
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.IOException

@HiltAndroidApp
class JurassicJournalApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        applyPendingDbUpdate()
        BundledDinoImages.init(this)
        BundledAbilityIcons.init(this)
    }

    /**
     * Logs image decode/fetch failures so intermittent load glitches (e.g. a dino
     * portrait momentarily failing to render) leave a diagnosable trail instead of
     * failing silently.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .eventListener(object : EventListener {
            override fun onError(request: ImageRequest, result: ErrorResult) {
                Log.w(TAG, "Image load failed for ${request.data}: ${result.throwable}")
            }
        })
        .build()

    /**
     * If a previously downloaded DB is staged, swap it into Room's database directory
     * before any Hilt singleton has a chance to open the database.
     */
    private fun applyPendingDbUpdate() {
        val prefs = getSharedPreferences(GameDataUpdater.PREFS_NAME, MODE_PRIVATE)
        val pendingVersion = prefs.getString(GameDataUpdater.KEY_PENDING_VERSION, null) ?: return
        val staged = File(filesDir, GameDataUpdater.STAGED_DB_FILE)
        if (!staged.exists()) {
            prefs.edit().remove(GameDataUpdater.KEY_PENDING_VERSION).apply()
            return
        }
        Log.d(TAG, "Applying staged update to $pendingVersion …")
        try {
            val dbFile = getDatabasePath("game_database")
            dbFile.parentFile?.mkdirs()
            // Copy to a temp file first so an interrupted copy never leaves the
            // live db file partially written; only rename() swaps it into place.
            val tmpDbFile = File(dbFile.parentFile, "${dbFile.name}.tmp")
            staged.copyTo(tmpDbFile, overwrite = true)
            // Remove WAL artifacts so Room opens the new file with a clean state
            File("${dbFile.path}-shm").delete()
            File("${dbFile.path}-wal").delete()
            if (!tmpDbFile.renameTo(dbFile)) {
                throw IOException("Failed to swap in updated database")
            }
            staged.delete()
            prefs.edit()
                .putString(GameDataUpdater.KEY_DATA_VERSION, pendingVersion)
                .remove(GameDataUpdater.KEY_PENDING_VERSION)
                .commit()
            Log.i(TAG, "Game database updated to $pendingVersion")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply staged update: ${e.message}")
            // Leave the staged file; will retry on the next launch
        }
    }

    companion object {
        private const val TAG = "JurassicJournalApp"
    }
}
