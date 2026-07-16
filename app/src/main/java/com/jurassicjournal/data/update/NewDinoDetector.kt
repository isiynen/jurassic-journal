package com.sufficienteffort.jurassicjournal.data.update

import android.content.Context
import com.sufficienteffort.jurassicjournal.data.game.dao.DinoDao
import com.sufficienteffort.jurassicjournal.data.user.dao.NewDinoDao
import com.sufficienteffort.jurassicjournal.data.user.dao.ProfileDao
import com.sufficienteffort.jurassicjournal.data.user.entity.NewDino
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewDinoDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dinoDao: DinoDao,
    private val newDinoDao: NewDinoDao,
    private val profileDao: ProfileDao,
) {
    companion object {
        private const val PREFS_NAME = "game_data_prefs"
        private const val KEY_KNOWN_SLUGS = "known_slugs"
    }

    suspend fun detect() {
        val currentSlugs = dinoDao.getAllSlugIds().map { it.slug }.toSet()
        // An empty slug set means the game DB isn't readable (mid-swap, corrupt);
        // proceeding would make pruneStale's NOT IN () delete every badge and
        // poison the baseline.
        if (currentSlugs.isEmpty()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val knownSlugs = prefs.getStringSet(KEY_KNOWN_SLUGS, null)

        if (knownSlugs == null) {
            // First install: establish baseline silently — do not flag any existing dinos
            prefs.edit().putStringSet(KEY_KNOWN_SLUGS, currentSlugs).apply()
            return
        }

        val newSlugs = currentSlugs - knownSlugs
        val entries = if (newSlugs.isNotEmpty()) {
            profileDao.getAll().flatMap { profile ->
                newSlugs.map { slug -> NewDino(profileId = profile.id, dinoSlug = slug) }
            }
        } else {
            emptyList()
        }

        // Insert badges + prune stale rows in one transaction, and only then
        // move the baseline forward — a crash in between just re-runs detection.
        newDinoDao.applyDetection(entries, currentSlugs.toList())
        prefs.edit().putStringSet(KEY_KNOWN_SLUGS, currentSlugs).apply()
    }
}
