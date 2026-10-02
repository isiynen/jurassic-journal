package com.sufficienteffort.jurassicjournal.data.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val tag: String,
    val downloadUrl: String,
    /** Asset size from the GitHub releases API; 0 if unknown. Used to detect truncated downloads. */
    val sizeBytes: Long = 0L,
)

@Singleton
class GameDataUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Checks GitHub for a newer release.
     * Returns [UpdateInfo] if one is available, null if already up to date,
     * no internet, or any error occurs.
     */
    suspend fun checkForUpdate(): UpdateInfo? {
        if (!isInternetAvailable()) {
            Log.d(TAG, "No internet — skipping update check")
            return null
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentVersion = prefs.getString(KEY_DATA_VERSION, BUNDLED_VERSION) ?: BUNDLED_VERSION
        Log.d(TAG, "Current data version: $currentVersion")

        // Throttle: this runs on every app launch; don't hammer the GitHub API.
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (now - lastCheck in 0 until CHECK_INTERVAL_MS) {
            Log.d(TAG, "Checked recently — skipping update check")
            return null
        }

        return try {
            val apiConn = URL(RELEASES_API_URL).openConnection() as HttpURLConnection
            val body = try {
                apiConn.setRequestProperty("Accept", "application/vnd.github+json")
                apiConn.connectTimeout = 8_000
                apiConn.readTimeout = 8_000
                apiConn.connect()

                val responseCode = apiConn.responseCode
                if (responseCode != 200) {
                    Log.d(TAG, "GitHub API returned HTTP $responseCode — no releases found")
                    return null
                }
                apiConn.inputStream.bufferedReader().readText()
            } finally {
                apiConn.disconnect()
            }
            prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

            // Enumerate all releases and pick the highest data tag matching our bundled SCHEMA.
            // /releases/latest is unreliable here: APK release tags (v1.x) can outrank data tags.
            val releases = JSONArray(body)
            var bestTag: String? = null
            var bestUrl: String? = null
            var bestSize = 0L
            for (i in 0 until releases.length()) {
                val release = releases.optJSONObject(i) ?: continue
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue
                val tag = release.optString("tag_name")
                if (tag.isEmpty()) continue
                // isNewer enforces the SCHEMA guard: same major, higher patch than current.
                if (!DataVersion.isNewer(tag, currentVersion)) continue
                if (bestTag != null && !DataVersion.isNewer(tag, bestTag)) continue

                val assets = release.optJSONArray("assets") ?: continue
                var url: String? = null
                var size = 0L
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    if (asset.optString("name") == DB_ASSET_NAME) {
                        url = asset.optString("browser_download_url").ifEmpty { null }
                        size = asset.optLong("size")
                        break
                    }
                }
                if (url == null) continue

                bestTag = tag
                bestUrl = url
                bestSize = size
            }

            if (bestTag == null || bestUrl == null) {
                Log.d(TAG, "Already up to date ($currentVersion) — no newer same-schema data release")
                return null
            }

            Log.d(TAG, "New data version available: $bestTag")
            UpdateInfo(tag = bestTag, downloadUrl = bestUrl, sizeBytes = bestSize)
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed: ${e::class.simpleName}: ${e.message}")
            null
        }
    }

    /**
     * Downloads the DB from [info] and stages it for the next launch.
     * Only writes the pending-version key after a complete, successful download.
     * Throws on any failure so the caller can surface an error.
     */
    suspend fun downloadAndStage(info: UpdateInfo, tracker: SyncProgressTracker? = null) {
        Log.d(TAG, "Downloading ${info.tag} …")
        tracker?.beginPhase(SyncPhase.DB_DOWNLOAD, total = 1)
        val staged = File(context.filesDir, STAGED_DB_FILE)
        try {
            downloadFile(info.downloadUrl, staged, info.sizeBytes, tracker)
        } catch (e: Exception) {
            tracker?.finish()
            throw e
        }

        // commit() is synchronous — must be flushed to disk before restartApp() kills the process
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_VERSION, info.tag).commit()
        Log.d(TAG, "Download complete — staged for next launch")
        tracker?.finish()
    }

    private fun isInternetAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return try {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Downloads to a .tmp file and only renames onto [dest] after the byte count
     * matches [expectedSize] and the content looks like a SQLite database.
     * A truncated-at-EOF body doesn't throw on read, so without these checks a
     * partial DB would get staged and swapped in on next launch.
     */
    private suspend fun downloadFile(
        url: String,
        dest: File,
        expectedSize: Long,
        tracker: SyncProgressTracker?,
    ) {
        val tmp = File(dest.parent, "${dest.name}.tmp")
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout    = 120_000
            conn.connect()
            if (conn.responseCode != 200) {
                throw IOException("HTTP ${conn.responseCode}")
            }
            val buffer = ByteArray(8_192)
            var pendingBytes = 0L
            var totalBytes = 0L
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        output.write(buffer, 0, n)
                        pendingBytes += n
                        totalBytes += n
                        if (pendingBytes >= PROGRESS_FLUSH_BYTES) {
                            tracker?.advance(bytes = pendingBytes)
                            pendingBytes = 0L
                        }
                    }
                }
            }
            if (pendingBytes > 0L) tracker?.advance(bytes = pendingBytes)

            if (expectedSize > 0L && totalBytes != expectedSize) {
                throw IOException("Truncated download: got $totalBytes of $expectedSize bytes")
            }
            if (!isSqliteFile(tmp)) {
                throw IOException("Downloaded file is not a SQLite database")
            }
            if (!tmp.renameTo(dest)) {
                throw IOException("Could not move ${tmp.name} to ${dest.name}")
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        } finally {
            conn.disconnect()
        }
    }

    private fun isSqliteFile(file: File): Boolean {
        val magic = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)
        if (file.length() < magic.size) return false
        val header = ByteArray(magic.size)
        file.inputStream().use { if (it.read(header) != magic.size) return false }
        return header.contentEquals(magic)
    }

    companion object {
        private const val TAG = "GameDataUpdater"
        private const val PROGRESS_FLUSH_BYTES = 262_144L

        const val PREFS_NAME          = "game_data_prefs"
        const val KEY_DATA_VERSION    = "data_version"
        const val KEY_PENDING_VERSION = "pending_version"
        const val KEY_LAST_CHECK      = "last_update_check"
        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L   // 6h
        const val STAGED_DB_FILE      = "game_db_pending.db"
        const val DB_ASSET_NAME       = "game_database.db"
        const val GAME_DB_NAME        = "game_database"

        // Format: data-vSCHEMA.PATCH  e.g. "data-v9.00" — see [DataVersion].
        // SCHEMA is a compatibility marker: only OTA releases with the same SCHEMA as this APK
        // are accepted. A higher SCHEMA means the data requires a newer APK (new image format,
        // schema-breaking change, etc). Bump SCHEMA here AND in release_data.py when introducing
        // incompatible data changes; reset PATCH to 00.
        const val BUNDLED_VERSION = "data-v9.27"

        // List endpoint (not /releases/latest): we enumerate and pick the highest data tag
        // matching our bundled SCHEMA. /latest is unreliable when APK and data tags coexist.
        const val RELEASES_API_URL =
            "https://api.github.com/repos/isiynen/jurassic-journal/releases?per_page=50"

        /**
         * Runs in Application.onCreate before Room opens anything:
         *  1. swaps in a staged OTA download, if one is pending;
         *  2. repairs a stored data version that no longer matches this APK's schema.
         */
        fun prepareGameDatabase(context: Context) {
            applyPendingDbUpdate(context)
            resetStaleDataVersion(context)
        }

        /**
         * If a previously downloaded DB is staged, swap it into Room's database directory.
         */
        private fun applyPendingDbUpdate(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pendingVersion = prefs.getString(KEY_PENDING_VERSION, null) ?: return
            val staged = File(context.filesDir, STAGED_DB_FILE)
            if (!staged.exists()) {
                prefs.edit().remove(KEY_PENDING_VERSION).apply()
                return
            }
            Log.d(TAG, "Applying staged update to $pendingVersion …")
            try {
                val dbFile = context.getDatabasePath(GAME_DB_NAME)
                dbFile.parentFile?.mkdirs()
                // Copy to a temp file first so an interrupted copy never leaves the
                // live db file partially written; only rename() swaps it into place.
                val tmpDbFile = File(dbFile.parentFile, "${dbFile.name}.tmp")
                staged.copyTo(tmpDbFile, overwrite = true)
                // WAL artifacts must go BEFORE the rename: a crash after the rename
                // with the old -wal still present would make SQLite "recover" the new
                // file with stale WAL pages. The reverse failure (rename fails, old DB
                // left without its WAL) is harmless — the game DB is read-only, so its
                // WAL never holds meaningful pages.
                File("${dbFile.path}-shm").delete()
                File("${dbFile.path}-wal").delete()
                if (!tmpDbFile.renameTo(dbFile)) {
                    throw IOException("Failed to swap in updated database")
                }
                staged.delete()
                prefs.edit()
                    .putString(KEY_DATA_VERSION, pendingVersion)
                    .remove(KEY_PENDING_VERSION)
                    .commit()
                Log.i(TAG, "Game database updated to $pendingVersion")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply staged update: ${e.message}")
                // Leave the staged file; will retry on the next launch
            }
        }

        /**
         * The stored data version outlives APK upgrades. After a SCHEMA bump the old
         * value (say `data-v9.30`) would make [DataVersion.isNewer] reject every
         * `data-v10.xx` release forever. When the stored schema differs from
         * [BUNDLED_VERSION], drop the on-device game DB so Room re-copies the bundled
         * asset, and reset the version to match it. An unparseable value (a locally
         * staged test DB) only resets the version, keeping the injected DB in place.
         */
        private fun resetStaleDataVersion(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY_DATA_VERSION, null) ?: return
            if (!DataVersion.isStale(stored, BUNDLED_VERSION)) return

            if (DataVersion.parse(stored) != null) {
                Log.i(TAG, "Stored data $stored is from another schema than bundled $BUNDLED_VERSION — recreating game DB from asset")
                context.deleteDatabase(GAME_DB_NAME)
            } else {
                Log.i(TAG, "Stored data version '$stored' is not a release tag — resetting to $BUNDLED_VERSION")
            }
            prefs.edit().putString(KEY_DATA_VERSION, BUNDLED_VERSION).apply()
        }
    }
}
