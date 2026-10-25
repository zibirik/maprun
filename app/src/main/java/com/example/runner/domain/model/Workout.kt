package com.example.runner.domain.model

/**
 * Сохранённая тренировка. Все величины — в СИ (метры, м/с, мс).
 */
data class Workout(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val durationMillis: Long,
    val avgPaceSecPerKm: Double,
    val avgSpeedMps: Double,
    val maxSpeedMps: Double,
    val calories: Int,
    val elevationGainMeters: Double,
    val points: List<TrackPoint> = emptyList(),
)
