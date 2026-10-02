package com.sufficienteffort.jurassicjournal.data.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Shared download engine behind [DinoImageSync] and [AbilityIconSync]: given the
 * relative paths referenced by the DB, fetches the ones missing locally from raw
 * GitHub. Paths that fail are recorded per DB version and skipped on later
 * launches; the skip list clears when the DB version changes so they retry.
 */
internal class AssetSyncEngine(
    private val context: Context,
    private val tracker: SyncProgressTracker,
    private val tag: String,
    private val phase: SyncPhase,
    private val rawBase: String,
    private val downloadDirName: String,
    private val failedPathsKey: String,
    private val failedVersionKey: String,
) {
    suspend fun sync(referencedPaths: List<String>) {
        val dir = File(context.filesDir, downloadDirName).also { it.mkdirs() }
        val knownFailed = loadFailedPaths()
        val pending = referencedPaths.filterNot { it in knownFailed }

        if (pending.isEmpty()) return
        if (pending.none { !File(dir, it).exists() }) return

        tracker.beginPhase(phase, total = pending.size)

        val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
        val results = try {
            coroutineScope {
                pending.map { path ->
                    async {
                        semaphore.withPermit { downloadOne(dir, path) }
                    }
                }.awaitAll()
            }
        } finally {
            tracker.finish()
        }

        val newlyFailed = results.filter { it.outcome == Outcome.FAILED }.map { it.path }.toSet()
        val downloaded = results.count { it.outcome == Outcome.DOWNLOADED }

        if (newlyFailed.isNotEmpty()) {
            saveFailedPaths(knownFailed + newlyFailed)
            Log.i(tag, "Marked ${newlyFailed.size} files as unavailable (will retry after next DB update)")
        }
        if (downloaded > 0) {
            Log.i(tag, "Sync: +$downloaded downloaded")
        }
    }

    private fun loadFailedPaths(): Set<String> {
        val prefs = context.getSharedPreferences(GameDataUpdater.PREFS_NAME, Context.MODE_PRIVATE)
        val currentDbVersion = prefs.getString(GameDataUpdater.KEY_DATA_VERSION, "") ?: ""
        val savedVersion = prefs.getString(failedVersionKey, "") ?: ""
        if (currentDbVersion != savedVersion) {
            prefs.edit().putString(failedVersionKey, currentDbVersion)
                .remove(failedPathsKey).apply()
            return emptySet()
        }
        return prefs.getStringSet(failedPathsKey, emptySet()) ?: emptySet()
    }

    private fun saveFailedPaths(paths: Set<String>) {
        val prefs = context.getSharedPreferences(GameDataUpdater.PREFS_NAME, Context.MODE_PRIVATE)
        val currentDbVersion = prefs.getString(GameDataUpdater.KEY_DATA_VERSION, "") ?: ""
        prefs.edit()
            .putString(failedVersionKey, currentDbVersion)
            .putStringSet(failedPathsKey, paths)
            .apply()
    }

    private enum class Outcome { EXISTED, DOWNLOADED, FAILED }
    private data class Result(val path: String, val outcome: Outcome)

    private suspend fun downloadOne(dir: File, path: String): Result {
        val dest = File(dir, path)
        if (dest.exists()) {
            tracker.advance(filesDelta = 1)
            return Result(path, Outcome.EXISTED)
        }
        val outcome = try {
            download(path, dest)
            Outcome.DOWNLOADED
        } catch (e: CancellationException) {
            // A cancelled download (activity destroyed mid-sync) must not land on
            // the per-version skip list — it would never retry until the next DB
            // update. Propagate so awaitAll cancels the whole sync.
            throw e
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch $path: ${e.message}")
            Outcome.FAILED
        }
        tracker.advance(filesDelta = 1)
        return Result(path, outcome)
    }

    private suspend fun download(path: String, dest: File) {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.parent, "${dest.name}.tmp")
        val conn = URL("$rawBase/$path").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout    = 30_000
            conn.connect()
            if (conn.responseCode != 200) {
                throw Exception("HTTP ${conn.responseCode}")
            }
            val buffer = ByteArray(8_192)
            var pendingBytes = 0L
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
            if (!tmp.renameTo(dest)) {
                throw Exception("Could not move ${tmp.name} into place")
            }
        } finally {
            conn.disconnect()
            if (tmp.exists()) tmp.delete()
        }
    }

    private companion object {
        const val MAX_CONCURRENT_DOWNLOADS = 5
        const val PROGRESS_FLUSH_BYTES = 262_144L
    }
}
