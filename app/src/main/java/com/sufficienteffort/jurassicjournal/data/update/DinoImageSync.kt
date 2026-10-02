package com.sufficienteffort.jurassicjournal.data.update

import android.content.Context
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DinoImageSync @Inject constructor(
    @ApplicationContext context: Context,
    private val dinoDao: DinoDao,
    tracker: SyncProgressTracker,
) {
    private val engine = AssetSyncEngine(
        context = context,
        tracker = tracker,
        tag = "DinoImageSync",
        phase = SyncPhase.IMAGE_SYNC,
        rawBase = DINO_IMAGE_RAW_BASE,
        downloadDirName = DOWNLOADED_DIR,
        failedPathsKey = "dino_image_failed_paths",
        failedVersionKey = "dino_image_failed_version",
    )

    /** Downloads any dino images referenced by the DB that are not bundled or already downloaded. */
    suspend fun syncMissingImages() {
        engine.sync(dinoDao.getAllImagePaths().filterNot { BundledDinoImages.contains(it) })
    }
}
