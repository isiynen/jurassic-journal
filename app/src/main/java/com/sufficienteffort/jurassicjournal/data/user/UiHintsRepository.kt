package com.sufficienteffort.jurassicjournal.data.user

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.uiHintsStore by preferencesDataStore(name = "ui_hints")

/** Remembers which one-time UI tips the user has already seen. */
@Singleton
class UiHintsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyModeSwitchSeen = booleanPreferencesKey("mode_switch_hint_seen")

    val modeSwitchHintSeen: Flow<Boolean> = context.uiHintsStore.data.map { it[keyModeSwitchSeen] ?: false }

    suspend fun markModeSwitchHintSeen() {
        context.uiHintsStore.edit { it[keyModeSwitchSeen] = true }
    }
}
