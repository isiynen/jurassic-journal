package com.sufficienteffort.jurassicjournal.data.user

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sufficienteffort.jurassicjournal.data.user.dao.ProfileDao
import com.sufficienteffort.jurassicjournal.data.user.entity.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class ActiveProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileDao: ProfileDao,
) {
    private val repairMutex = Mutex()
    private val KEY_ACTIVE_PROFILE = longPreferencesKey("active_profile_id")

    val activeProfileId: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[KEY_ACTIVE_PROFILE] ?: 1L }

    suspend fun setActiveProfile(id: Long) {
        context.dataStore.edit { it[KEY_ACTIVE_PROFILE] = id }
    }

    /**
     * Returns a profile id guaranteed to exist, repairing inconsistent state
     * before a profile-scoped insert. The stored active id can be stale (the
     * default 1L before the flow emits) or point at a deleted profile, either
     * of which would trip a foreign-key constraint. Resolution order:
     *   1. the active id, if its row exists;
     *   2. the first existing profile, promoted to active;
     *   3. a freshly created "Default" profile.
     */
    suspend fun requireActiveProfileId(): Long = repairMutex.withLock {
        // Mutex: two concurrent callers both seeing "no profile" would each
        // insert a Default profile (non-atomic check-then-insert).
        val activeId = activeProfileId.first()
        if (profileDao.getById(activeId) != null) return activeId

        val fallback = profileDao.getAll().firstOrNull()
        if (fallback != null) {
            setActiveProfile(fallback.id)
            return fallback.id
        }

        val newId = profileDao.insert(Profile(name = "Default"))
        setActiveProfile(newId)
        return newId
    }

    fun observeProfiles(): Flow<List<Profile>> = profileDao.observeAll()

    suspend fun getProfileById(id: Long): Profile? = profileDao.getById(id)

    suspend fun createProfile(name: String): Long =
        profileDao.insert(Profile(name = name))

    suspend fun renameProfile(id: Long, newName: String) {
        profileDao.getById(id)?.let { profileDao.update(it.copy(name = newName)) }
    }

    /** Deletes the profile and all rows it owns (wallet, collection, boosts, …) atomically. */
    suspend fun deleteProfile(id: Long) {
        profileDao.deleteProfileWithData(id)
    }

    /** Cleans rows orphaned by profile deletions from before deleteProfileWithData existed. */
    suspend fun pruneOrphanedData() {
        profileDao.pruneOrphanedData()
    }
}
