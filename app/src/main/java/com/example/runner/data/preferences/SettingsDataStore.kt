package com.example.runner.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.runner.domain.model.AppThemeMode
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.UserSettings
import com.example.runner.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "runner_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {

    private val dataStore = context.settingsDataStore

    private object Keys {
        val unit = stringPreferencesKey("unit")
        val weight = floatPreferencesKey("weight_kg")
        val theme = stringPreferencesKey("theme")
        val voiceKm = floatPreferencesKey("voice_every_km")
        val autoPause = booleanPreferencesKey("auto_pause")
        val challengeDay = longPreferencesKey("challenge_completed_day")
    }

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            unit = prefs[Keys.unit]?.let { runCatching { DistanceUnit.valueOf(it) }.getOrNull() }
                ?: DistanceUnit.KILOMETERS,
            weightKg = prefs[Keys.weight] ?: 70f,
            themeMode = prefs[Keys.theme]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM,
            voiceEveryKm = prefs[Keys.voiceKm] ?: 1f,
            autoPauseEnabled = prefs[Keys.autoPause] ?: true,
        )
    }

    override suspend fun setUnit(unit: DistanceUnit) {
        dataStore.edit { it[Keys.unit] = unit.name }
    }

    override suspend fun setWeightKg(weight: Float) {
        dataStore.edit { it[Keys.weight] = weight.coerceIn(30f, 250f) }
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[Keys.theme] = mode.name }
    }

    override suspend fun setVoiceEveryKm(km: Float) {
        dataStore.edit { it[Keys.voiceKm] = km }
    }

    override suspend fun setAutoPause(enabled: Boolean) {
        dataStore.edit { it[Keys.autoPause] = enabled }
    }

    override suspend fun markChallengeCompleted(epochDay: Long) {
        dataStore.edit { it[Keys.challengeDay] = epochDay }
    }

    override suspend fun lastCompletedChallengeDay(): Long? {
        return dataStore.data.first()[Keys.challengeDay]
    }
}
