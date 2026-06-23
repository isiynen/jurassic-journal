package com.jurassicjournal.data.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

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
                Log.d(TAG, "GitHub API returned HTTP $responseCode — no release found")
                return null
            }

            val body = apiConn.inputStream.bufferedReader().readText()
            apiConn.disconnect()

            val json = JSONObject(body)
            val tagName = json.optString("tag_name").ifEmpty {
                Log.d(TAG, "Release has no tag_name — skipping")
                return null
            }
            Log.d(TAG, "Latest release: $tagName")

            if (!isNewer(tagName, currentVersion)) {
                Log.d(TAG, "Already up to date ($currentVersion)")
                return null
            }

            Log.d(TAG, "New version available: $tagName — looking for $DB_ASSET_NAME")
            val assets = json.optJSONArray("assets") ?: run {
                Log.d(TAG, "No assets in release")
                return null
            }
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == DB_ASSET_NAME) {
                    downloadUrl = asset.optString("browser_download_url").ifEmpty { null }
                    break
                }
            }
            if (downloadUrl == null) {
                Log.d(TAG, "$DB_ASSET_NAME not found in release assets")
                return null
            }

            UpdateInfo(tag = tagName, downloadUrl = downloadUrl)
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
    suspend fun downloadAndStage(info: UpdateInfo) {
        Log.d(TAG, "Downloading ${info.tag} …")
        val staged = File(context.filesDir, STAGED_DB_FILE)
        downloadFile(info.downloadUrl, staged)

        // commit() is synchronous — must be flushed to disk before restartApp() kills the process
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_VERSION, info.tag).commit()
        Log.d(TAG, "Download complete — staged for next launch")
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

    private fun downloadFile(url: String, dest: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout    = 120_000
        conn.connect()
        if (conn.responseCode != 200) {
            conn.disconnect()
            throw Exception("HTTP ${conn.responseCode}")
        }
        conn.inputStream.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
        conn.disconnect()
    }

    companion object {
        private const val TAG = "GameDataUpdater"

        const val PREFS_NAME          = "game_data_prefs"
        const val KEY_DATA_VERSION    = "data_version"
        const val KEY_PENDING_VERSION = "pending_version"
        const val STAGED_DB_FILE      = "game_db_pending.db"
        const val DB_ASSET_NAME       = "game_database.db"

        // Format: data-vSCHEMA.DATA  e.g. "data-v7.00"
        // SCHEMA matches the Room GameDatabase version — only bumps with schema changes + new APK.
        // DATA is the OTA patch counter within that schema; auto-incremented by the pipeline.
        const val BUNDLED_VERSION = "data-v8.00"

        const val RELEASES_API_URL =
            "https://api.github.com/repos/isiynen/jurassic-journal/releases/latest"

        fun isNewer(candidate: String, current: String): Boolean {
            val (cMajor, cMinor) = parseVersion(candidate) ?: return false
            val (kMajor, kMinor) = parseVersion(current)   ?: return false
            return cMajor > kMajor || (cMajor == kMajor && cMinor > kMinor)
        }

        private fun parseVersion(tag: String): Pair<Int, Int>? {
            val m = Regex("""data-v(\d+)\.(\d+)""").matchEntire(tag) ?: return null
            return m.groupValues[1].toInt() to m.groupValues[2].toInt()
        }
    }
}
