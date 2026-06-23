package com.jurassicjournal.data.update

import android.content.Context
import android.util.Log
import com.jurassicjournal.data.game.dao.MoveDao
import dagger.hilt.android.qualifiers.ApplicationContext
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
     * Covers both main icons and overlay/badge icons (shared/).
     * Skips bundled APK icons and already-downloaded files; counter still advances for skips
     * so resume progress is visible.
     */
    suspend fun syncMissingIcons() {
        val dir = File(context.filesDir, ABILITY_DOWNLOADED_DIR).also { it.mkdirs() }
        val pending = collectPendingIcons()

        if (pending.isEmpty()) return

        tracker.beginPhase(SyncPhase.ABILITY_SYNC, total = pending.size)

        var downloaded = 0
        var failed = 0
        for (rel in pending) {
            val dest = File(dir, rel)
            if (dest.exists()) {
                tracker.advance(filesDelta = 1)
                continue
            }
            try {
                downloadIcon(rel, dest)
                downloaded++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch ability icon $rel: ${e.message}")
                failed++
            }
            tracker.advance(filesDelta = 1)
        }

        tracker.finish()
        if (downloaded > 0 || failed > 0) {
            Log.i(TAG, "Ability icon sync: +$downloaded downloaded, $failed failed")
        }
    }

    /** Collect distinct icon relative paths (e.g. "shared/priority.png") not in the bundled APK. */
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
        try {
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output ->
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        output.write(buffer, 0, n)
                        tracker.advance(bytes = n.toLong())
                    }
                }
            }
            tmp.renameTo(dest)
        } finally {
            conn.disconnect()
            if (tmp.exists()) tmp.delete()
        }
    }

    companion object {
        private const val TAG = "AbilityIconSync"
    }
}
