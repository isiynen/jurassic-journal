package com.jurassicjournal.data.update

import android.content.Context
import java.io.File

internal const val DOWNLOADED_DIR = "dino_images"
internal const val DINO_IMAGE_RAW_BASE =
    "https://raw.githubusercontent.com/isiynen/jurassic-journal/master/app/src/main/assets/dinosaurs"

/**
 * Cached set of image filenames bundled in the APK's assets/dinosaurs/ directory.
 * Must be initialised once in Application.onCreate() before any image is loaded.
 */
object BundledDinoImages {
    private var names: Set<String> = emptySet()

    fun init(context: Context) {
        names = context.assets.list("dinosaurs")?.toHashSet() ?: emptySet()
    }

    fun contains(imagePath: String): Boolean = imagePath in names
}

/**
 * Resolves a dino imagePath to the best available source, in priority order:
 *   1. Proactively downloaded file in filesDir/dino_images/ (OTA-synced, offline-ready)
 *   2. Bundled APK asset (fast, no network)
 *   3. Raw GitHub URL (Coil fetches and caches; used for dinos added after this APK was built)
 *
 * Pass the result directly to ImageRequest.Builder.data().
 */
fun dinoImageModel(context: Context, imagePath: String): Any {
    val downloaded = File(context.filesDir, "$DOWNLOADED_DIR/$imagePath")
    if (downloaded.exists()) return downloaded
    if (BundledDinoImages.contains(imagePath)) return "file:///android_asset/dinosaurs/$imagePath"
    return "$DINO_IMAGE_RAW_BASE/$imagePath"
}
