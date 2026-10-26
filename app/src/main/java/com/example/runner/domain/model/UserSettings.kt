package com.example.runner.domain.model

enum class DistanceUnit { KILOMETERS, MILES }

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val unit: DistanceUnit = DistanceUnit.KILOMETERS,
    val weightKg: Float = 70f,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val voiceEveryKm: Float = 1f,
    val autoPauseEnabled: Boolean = true,
    val weeklyGoalKm: Float = 20f,
)
