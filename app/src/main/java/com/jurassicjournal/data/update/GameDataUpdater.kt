package com.jurassicjournal.data.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.jurassicjournal.data.update.SyncPhase
import com.jurassicjournal.data.update.SyncProgressTracker

data class UpdateInfo(val tag: String, val downloadUrl: String)

class GameDataUpdater(private val context: Context) {

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

        return try {
            val apiConn = URL(RELEASES_API_URL).openConnection() as HttpURLConnection
            apiConn.setRequestProperty("Accept", "application/vnd.github+json")
            apiConn.connectTimeout = 8_000
            apiConn.readTimeout = 8_000
            apiConn.connect()

            val responseCode = apiConn.responseCode
            if (responseCode != 200) {
                apiConn.disconnect()
                Log.d(TAG, "GitHub API returned HTTP $responseCode — no releases found")
                return null
            }

            val body = apiConn.inputStream.bufferedReader().readText()
            apiConn.disconnect()

            // Enumerate all releases and pick the highest data tag matching our bundled SCHEMA.
            // /releases/latest is unreliable here: APK release tags (v1.x) can outrank data tags.
            val releases = JSONArray(body)
            var bestTag: String? = null
            var bestUrl: String? = null
            for (i in 0 until releases.length()) {
                val release = releases.optJSONObject(i) ?: continue
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue
                val tag = release.optString("tag_name")
                if (tag.isEmpty()) continue
                // isNewer enforces the SCHEMA guard: same major, higher patch than current.
                if (!isNewer(tag, currentVersion)) continue
                if (bestTag != null && !isNewer(tag, bestTag)) continue

                val assets = release.optJSONArray("assets") ?: continue
                var url: String? = null
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    if (asset.optString("name") == DB_ASSET_NAME) {
                        url = asset.optString("browser_download_url").ifEmpty { null }
                        break
                    }
                }
                if (url == null) continue

                bestTag = tag
                bestUrl = url
            }

            if (bestTag == null || bestUrl == null) {
                Log.d(TAG, "Already up to date ($currentVersion) — no newer same-schema data release")
                return null
            }

            Log.d(TAG, "New data version available: $bestTag")
            UpdateInfo(tag = bestTag, downloadUrl = bestUrl)
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
            downloadFile(info.downloadUrl, staged, tracker)
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

    private suspend fun downloadFile(url: String, dest: File, tracker: SyncProgressTracker?) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout    = 120_000
        conn.connect()
        if (conn.responseCode != 200) {
            conn.disconnect()
            throw Exception("HTTP ${conn.responseCode}")
        }
        val buffer = ByteArray(8_192)
        var pendingBytes = 0L
        conn.inputStream.use { input ->
            FileOutputStream(dest).use { output ->
                var n: Int
                while (input.read(buffer).also { n = it } != -1) {
                    output.write(buffer, 0, n)
                    pendingBytes += n
                    if (pendingBytes >= PROGRESS_FLUSH_BYTES) {
                        tracker?.advance(bytes = pendingBytes)
                        pendingBytes = 0L
                    }
                }
            }
        }
        if (pendingBytes > 0L) tracker?.advance(bytes = pendingBytes)
        conn.disconnect()
    }

    companion object {
        private const val TAG = "GameDataUpdater"
        private const val PROGRESS_FLUSH_BYTES = 262_144L

        const val PREFS_NAME          = "game_data_prefs"
        const val KEY_DATA_VERSION    = "data_version"
        const val KEY_PENDING_VERSION = "pending_version"
        const val STAGED_DB_FILE      = "game_db_pending.db"
        const val DB_ASSET_NAME       = "game_database.db"

        // Format: data-vSCHEMA.DATA  e.g. "data-v7.00"
        // SCHEMA matches the Room GameDatabase version — only bumps with schema changes + new APK.
        // DATA is the OTA patch counter within that schema; auto-incremented by the pipeline.
        // Format: data-vSCHEMA.PATCH  e.g. "data-v9.00"
        // SCHEMA is a compatibility marker: only OTA releases with the same SCHEMA as this APK
        // are accepted. A higher SCHEMA means the data requires a newer APK (new image format,
        // schema-breaking change, etc). Bump SCHEMA here AND in release_data.py when introducing
        // incompatible data changes; reset PATCH to 00.
        const val BUNDLED_VERSION = "data-v9.08"

        // List endpoint (not /releases/latest): we enumerate and pick the highest data tag
        // matching our bundled SCHEMA. /latest is unreliable when APK and data tags coexist.
        const val RELEASES_API_URL =
            "https://api.github.com/repos/isiynen/jurassic-journal/releases?per_page=50"

        fun isNewer(candidate: String, current: String): Boolean {
            val (cMajor, cMinor) = parseVersion(candidate) ?: return false
            val (kMajor, kMinor) = parseVersion(current)   ?: return false
            // Only accept patches within the same SCHEMA. A higher SCHEMA requires a new APK.
            if (cMajor != kMajor) return false
            return cMinor > kMinor
        }

        private fun parseVersion(tag: String): Pair<Int, Int>? {
            val m = Regex("""data-v(\d+)\.(\d+)""").matchEntire(tag) ?: return null
            return m.groupValues[1].toInt() to m.groupValues[2].toInt()
        }
    }
}
