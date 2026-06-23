package com.jurassicjournal.data.update

import android.content.Context
import android.util.Log
import com.jurassicjournal.data.game.dao.DinoDao
import dagger.hilt.android.qualifiers.ApplicationContext
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
     * Skips bundled APK images and already-downloaded files.
     * As skipped files are counted, the progress counter advances quickly before slowing
     * to actual download speed for genuinely missing images.
     */
    suspend fun syncMissingImages() {
        val dir = File(context.filesDir, DOWNLOADED_DIR).also { it.mkdirs() }
        val allPaths = dinoDao.getAllImagePaths()
        val pending = allPaths.filter { !BundledDinoImages.contains(it) }

        if (pending.isEmpty()) return

        tracker.beginPhase(SyncPhase.IMAGE_SYNC, total = pending.size)

        var downloaded = 0
        var failed = 0
        for (path in pending) {
            val dest = File(dir, path)
            if (dest.exists()) {
                // Already downloaded in a previous session — advance counter without bytes
                tracker.advance(filesDelta = 1)
                continue
            }
            try {
                downloadImage(path, dest)
                downloaded++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch $path: ${e.message}")
                failed++
            }
            tracker.advance(filesDelta = 1)
        }

        tracker.finish()
        if (downloaded > 0 || failed > 0) {
            Log.i(TAG, "Image sync: +$downloaded downloaded, $failed failed")
        }
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
        private const val TAG = "DinoImageSync"
    }
}
