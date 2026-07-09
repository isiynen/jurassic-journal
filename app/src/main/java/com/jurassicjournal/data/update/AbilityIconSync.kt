package com.sufficienteffort.jurassicjournal.data.update

import android.content.Context
import android.util.Log
import com.sufficienteffort.jurassicjournal.data.game.dao.MoveDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AbilityIconSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moveDao: MoveDao,
    private val tracker: SyncProgressTracker,
) {
    /**
     * Downloads any ability icons referenced by the DB that are not yet available locally.
     * Icons that repeatedly fail (not yet on server) are recorded per DB version and skipped
     * on subsequent launches to avoid a persistent strip. The skip list is cleared whenever
     * the DB version changes so new pushes are retried automatically.
     */
    suspend fun syncMissingIcons() {
        val dir = File(context.filesDir, ABILITY_DOWNLOADED_DIR).also { it.mkdirs() }
        val knownFailed = loadFailedPaths()
        val pending = collectPendingIcons().filterNot { it in knownFailed }

        if (pending.isEmpty()) return
        if (pending.none { !File(dir, it).exists() }) return

        tracker.beginPhase(SyncPhase.ABILITY_SYNC, total = pending.size)

        val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
        val results = coroutineScope {
            pending.map { rel ->
                async {
                    semaphore.withPermit { downloadOne(dir, rel) }
                }
            }.awaitAll()
        }

        val newlyFailed = results.filter { it.outcome == IconOutcome.FAILED }.map { it.path }.toSet()
        val downloaded = results.count { it.outcome == IconOutcome.DOWNLOADED }

        tracker.finish()

        if (newlyFailed.isNotEmpty()) {
            saveFailedPaths(knownFailed + newlyFailed)
            Log.i(TAG, "Marked ${newlyFailed.size} icons as unavailable (will retry after next DB update)")
        }
        if (downloaded > 0) {
            Log.i(TAG, "Ability icon sync: +$downloaded downloaded")
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

    private suspend fun collectPendingIcons(): List<String> {
        val iconData = moveDao.getAllIconData()
        val allRel = mutableSetOf<String>()

        for (row in iconData) {
            row.mainIconPath?.let { path ->
                allRel += path.removePrefix("icons/")
            }
            row.overlayIconsJson?.let { json ->
                runCatching {
                    val arr = JSONArray(json)
                    for (i in 0 until arr.length()) {
                        val path = arr.getJSONObject(i).optString("path")
                        if (path.isNotEmpty()) allRel += path.removePrefix("icons/")
                    }
                }
            }
        }

        return allRel.filterNot { BundledAbilityIcons.contains(it) }.sorted()
    }

    private enum class IconOutcome { EXISTED, DOWNLOADED, FAILED }
    private data class IconResult(val path: String, val outcome: IconOutcome)

    private suspend fun downloadOne(dir: File, rel: String): IconResult {
        val dest = File(dir, rel)
        if (dest.exists()) {
            tracker.advance(filesDelta = 1)
            return IconResult(rel, IconOutcome.EXISTED)
        }
        val outcome = try {
            downloadIcon(rel, dest)
            IconOutcome.DOWNLOADED
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch ability icon $rel: ${e.message}")
            IconOutcome.FAILED
        }
        tracker.advance(filesDelta = 1)
        return IconResult(rel, outcome)
    }

    private suspend fun downloadIcon(rel: String, dest: File) {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parent, "${dest.name}.tmp")
        val conn = URL("$ABILITY_ICON_RAW_BASE/$rel").openConnection() as HttpURLConnection
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
        private const val TAG = "AbilityIconSync"
        private const val KEY_FAILED_PATHS   = "ability_icon_failed_paths"
        private const val KEY_FAILED_VERSION = "ability_icon_failed_version"
        private const val MAX_CONCURRENT_DOWNLOADS = 5
        private const val PROGRESS_FLUSH_BYTES = 262_144L
    }
}
