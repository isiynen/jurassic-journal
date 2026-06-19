package com.jurassicjournal.data.user

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jurassicjournal.data.user.dao.ProfileDao
import com.jurassicjournal.data.user.entity.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class ActiveProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    val profileDao: ProfileDao,
) {
    private val KEY_ACTIVE_PROFILE = longPreferencesKey("active_profile_id")

    val activeProfileId: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[KEY_ACTIVE_PROFILE] ?: 1L }

    suspend fun setActiveProfile(id: Long) {
        context.dataStore.edit { it[KEY_ACTIVE_PROFILE] = id }
    }

    fun observeProfiles(): Flow<List<Profile>> = profileDao.observeAll()

    suspend fun createProfile(name: String): Long =
        profileDao.insert(Profile(name = name))

    suspend fun renameProfile(id: Long, newName: String) {
        profileDao.getById(id)?.let { profileDao.update(it.copy(name = newName)) }
    }

    suspend fun deleteProfile(id: Long) {
        profileDao.deleteById(id)
    }
}
