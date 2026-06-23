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
) {
    /**
     * Downloads any dino images referenced by the DB that are not yet available locally
     * (neither bundled in the APK nor previously downloaded).
     *
     * Safe to run on every launch — file-existence checks make it a no-op when up to date.
     * Run on an IO dispatcher.
     */
    suspend fun syncMissingImages() {
        val dir = File(context.filesDir, DOWNLOADED_DIR).also { it.mkdirs() }
        val paths = dinoDao.getAllImagePaths()

        var downloaded = 0
        var failed = 0
        for (path in paths) {
            if (BundledDinoImages.contains(path)) continue
            val dest = File(dir, path)
            if (dest.exists()) continue
            try {
                downloadImage(path, dest)
                downloaded++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch $path: ${e.message}")
                failed++
            }
        }

        if (downloaded > 0 || failed > 0) {
            Log.i(TAG, "Image sync: +$downloaded downloaded, $failed failed")
        }
    }

    private fun downloadImage(imagePath: String, dest: File) {
        val tmp = File(dest.parent, "${dest.name}.tmp")
        val conn = URL("$DINO_IMAGE_RAW_BASE/$imagePath").openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout    = 30_000
        conn.connect()
        if (conn.responseCode != 200) {
            conn.disconnect()
            throw Exception("HTTP ${conn.responseCode}")
        }
        try {
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
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
