package com.sufficienteffort.jurassicjournal.data.update

import android.content.Context
import com.sufficienteffort.jurassicjournal.data.game.dao.MoveDao
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AbilityIconSync @Inject constructor(
    @ApplicationContext context: Context,
    private val moveDao: MoveDao,
    tracker: SyncProgressTracker,
) {
    private val engine = AssetSyncEngine(
        context = context,
        tracker = tracker,
        tag = "AbilityIconSync",
        phase = SyncPhase.ABILITY_SYNC,
        rawBase = ABILITY_ICON_RAW_BASE,
        downloadDirName = ABILITY_DOWNLOADED_DIR,
        failedPathsKey = "ability_icon_failed_paths",
        failedVersionKey = "ability_icon_failed_version",
    )

    /** Downloads any ability icons referenced by the DB that are not bundled or already downloaded. */
    suspend fun syncMissingIcons() {
        engine.sync(collectPendingIcons())
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
}
