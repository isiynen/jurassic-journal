package com.jurassicjournal.data.update

import android.content.Context
import android.util.Log
import com.jurassicjournal.data.game.dao.DinoDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DinoImageSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dinoDao: DinoDao,
    private val tracker: SyncProgressTracker,
) {
    /**
     * Downloads any dino images referenced by the DB that are not yet available locally.
     * Images that repeatedly fail are recorded per DB version and skipped on subsequent
     * launches. The skip list clears when the DB version changes so they retry automatically.
     */
    suspend fun syncMissingImages() {
        val dir = File(context.filesDir, DOWNLOADED_DIR).also { it.mkdirs() }
        val allPaths = dinoDao.getAllImagePaths()
        val knownFailed = loadFailedPaths()
        val pending = allPaths
            .filter { !BundledDinoImages.contains(it) }
            .filterNot { it in knownFailed }

        if (pending.isEmpty()) return
        if (pending.none { !File(dir, it).exists() }) return

        tracker.beginPhase(SyncPhase.IMAGE_SYNC, total = pending.size)

        val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
        val results = coroutineScope {
            pending.map { path ->
                async {
                    semaphore.withPermit { downloadOne(dir, path) }
                }
            }.awaitAll()
        }

        val newlyFailed = results.filter { it.outcome == ImageOutcome.FAILED }.map { it.path }.toSet()
        val downloaded = results.count { it.outcome == ImageOutcome.DOWNLOADED }

        tracker.finish()

        if (newlyFailed.isNotEmpty()) {
            saveFailedPaths(knownFailed + newlyFailed)
            Log.i(TAG, "Marked ${newlyFailed.size} images as unavailable (will retry after next DB update)")
        }
        if (downloaded > 0) {
            Log.i(TAG, "Image sync: +$downloaded downloaded")
        }
    }

    private fun loadFailedPaths(): Set<String> {
        val prefs = context.getSharedPreferences(GameDataUpdater.PREFS_NAME, Context.MODE_PRIVATE)
        val currentDbVersion = prefs.getString(GameDataUpdater.KEY_DATA_VERSION, "") ?: ""
        val savedVersion = prefs.getString(KEY_FAILED_VERSION, "") ?: ""
        if (currentDbVersion != savedVersion) {
            prefs.edit().putString(KEY_FAILED_VERSION, currentDbVersion)
                .remove(KEY_FAILED_PATHS).apply()
            return emptySet()
        }
        return prefs.getStringSet(KEY_FAILED_PATHS, emptySet()) ?: emptySet()
    }

    private fun saveFailedPaths(paths: Set<String>) {
        val prefs = context.getSharedPreferences(GameDataUpdater.PREFS_NAME, Context.MODE_PRIVATE)
        val currentDbVersion = prefs.getString(GameDataUpdater.KEY_DATA_VERSION, "") ?: ""
        prefs.edit()
            .putString(KEY_FAILED_VERSION, currentDbVersion)
            .putStringSet(KEY_FAILED_PATHS, paths)
            .apply()
    }

    private enum class ImageOutcome { EXISTED, DOWNLOADED, FAILED }
    private data class ImageResult(val path: String, val outcome: ImageOutcome)

    private suspend fun downloadOne(dir: File, path: String): ImageResult {
        val dest = File(dir, path)
        if (dest.exists()) {
            tracker.advance(filesDelta = 1)
            return ImageResult(path, ImageOutcome.EXISTED)
        }
        val outcome = try {
            downloadImage(path, dest)
            ImageOutcome.DOWNLOADED
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch $path: ${e.message}")
            ImageOutcome.FAILED
        }
        tracker.advance(filesDelta = 1)
        return ImageResult(path, outcome)
    }

    private suspend fun downloadImage(imagePath: String, dest: File) {
        val tmp = File(dest.parent, "${dest.name}.tmp")
        val conn = URL("$DINO_IMAGE_RAW_BASE/$imagePath").openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout    = 30_000
        conn.connect()
        if (conn.responseCode != 200) {
            conn.disconnect()
            throw Exception("HTTP ${conn.responseCode}")
        }
        val buffer = ByteArray(8_192)
        var pendingBytes = 0L
        try {
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        output.write(buffer, 0, n)
                        pendingBytes += n
                        if (pendingBytes >= PROGRESS_FLUSH_BYTES) {
                            tracker.advance(bytes = pendingBytes)
                            pendingBytes = 0L
                        }
                    }
                }
            }
            if (pendingBytes > 0L) tracker.advance(bytes = pendingBytes)
            tmp.renameTo(dest)
        } finally {
            conn.disconnect()
            if (tmp.exists()) tmp.delete()
        }
    }

    companion object {
        private const val TAG = "DinoImageSync"
        private const val KEY_FAILED_PATHS   = "dino_image_failed_paths"
        private const val KEY_FAILED_VERSION = "dino_image_failed_version"
        private const val MAX_CONCURRENT_DOWNLOADS = 5
        private const val PROGRESS_FLUSH_BYTES = 262_144L
    }
}
