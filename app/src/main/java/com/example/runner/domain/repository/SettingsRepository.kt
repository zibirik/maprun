package com.example.runner.domain.repository

import com.example.runner.domain.model.AppThemeMode
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun setUnit(unit: DistanceUnit)
    suspend fun setWeightKg(weight: Float)
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setVoiceEveryKm(km: Float)
    suspend fun setAutoPause(enabled: Boolean)
    suspend fun setWeeklyGoalKm(km: Float)
    suspend fun markChallengeCompleted(epochDay: Long)
    suspend fun lastCompletedChallengeDay(): Long?
}
