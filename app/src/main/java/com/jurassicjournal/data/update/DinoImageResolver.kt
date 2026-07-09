package com.sufficienteffort.jurassicjournal.data.update

import android.content.Context
import java.io.File

// --- Dino images ---

internal const val DOWNLOADED_DIR = "dino_images"
internal const val DINO_IMAGE_RAW_BASE =
    "https://raw.githubusercontent.com/isiynen/jurassic-journal/master/app/src/main/assets/dinosaurs"

object BundledDinoImages {
    private var names: Set<String> = emptySet()
    fun init(context: Context) { names = context.assets.list("dinosaurs")?.toHashSet() ?: emptySet() }
    fun contains(imagePath: String): Boolean = imagePath in names
}

/**
 * Resolves a dino imagePath to the best available source:
 *   1. Downloaded file in filesDir/dino_images/
 *   2. Bundled APK asset
 *   3. Raw GitHub URL (for dinos added after this APK was built)
 */
fun dinoImageModel(context: Context, imagePath: String): Any {
    val downloaded = File(context.filesDir, "$DOWNLOADED_DIR/$imagePath")
    if (downloaded.exists()) return downloaded
    if (BundledDinoImages.contains(imagePath)) return "file:///android_asset/dinosaurs/$imagePath"
    return "$DINO_IMAGE_RAW_BASE/$imagePath"
}

// --- Ability icons ---

internal const val ABILITY_DOWNLOADED_DIR = "ability_icons"
internal const val ABILITY_ICON_RAW_BASE =
    "https://raw.githubusercontent.com/isiynen/jurassic-journal/master/app/src/main/assets/ability_icons"

/**
 * Cached set of icon filenames (relative paths like "shared/priority.png") bundled in the APK.
 * Must be initialised once in Application.onCreate() before any icon is loaded.
 */
object BundledAbilityIcons {
    private var names: Set<String> = emptySet()

    fun init(context: Context) {
        val flat   = context.assets.list("ability_icons")?.filter { it.endsWith(".png") } ?: emptyList()
        val shared = context.assets.list("ability_icons/shared")
            ?.filter { it.endsWith(".png") }
            ?.map { "shared/$it" }
            ?: emptyList()
        names = (flat + shared).toHashSet()
    }

    fun contains(relativePath: String): Boolean = relativePath in names
}

/**
 * Resolves an ability icon path (e.g. "icons/bellowing_shielded_advantage.png" or
 * "icons/shared/priority.png") to the best available source:
 *   1. Downloaded file in filesDir/ability_icons/
 *   2. Bundled APK asset
 *   3. Raw GitHub URL (for abilities added after this APK was built)
 *
 * [rawPath] is exactly as stored in the DB/JSON (prefixed with "icons/").
 */
fun abilityIconModel(context: Context, rawPath: String): Any {
    val rel = rawPath.removePrefix("icons/")   // e.g. "bellowing_shielded_advantage.png" or "shared/priority.png"
    val downloaded = File(context.filesDir, "$ABILITY_DOWNLOADED_DIR/$rel")
    if (downloaded.exists()) return downloaded
    if (BundledAbilityIcons.contains(rel)) return "file:///android_asset/ability_icons/$rel"
    return "$ABILITY_ICON_RAW_BASE/$rel"
}
