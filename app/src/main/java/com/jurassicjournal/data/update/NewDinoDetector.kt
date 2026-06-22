package com.jurassicjournal.data.update

import android.content.Context
import com.jurassicjournal.data.game.dao.DinoDao
import com.jurassicjournal.data.user.dao.NewDinoDao
import com.jurassicjournal.data.user.dao.ProfileDao
import com.jurassicjournal.data.user.entity.NewDino
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val knownSlugs = prefs.getStringSet(KEY_KNOWN_SLUGS, null)

        if (knownSlugs == null) {
            // First install: establish baseline silently — do not flag any existing dinos
            prefs.edit().putStringSet(KEY_KNOWN_SLUGS, currentSlugs).apply()
            return
        }

        val newSlugs = currentSlugs - knownSlugs
        if (newSlugs.isNotEmpty()) {
            val profiles = profileDao.getAll()
            val entries = profiles.flatMap { profile ->
                newSlugs.map { slug -> NewDino(profileId = profile.id, dinoSlug = slug) }
            }
            newDinoDao.insertAll(entries)
        }

        // Remove rows for slugs no longer in the game DB
        newDinoDao.pruneStale(currentSlugs.toList())

        // Update baseline to current set
        prefs.edit().putStringSet(KEY_KNOWN_SLUGS, currentSlugs).apply()
    }
}
