package com.example.runner.domain.model

/**
 * Одна GPS-точка трека. Хранится в метрической системе.
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val speedMps: Double,
    val altitude: Double,
    val accuracyMeters: Float,
)
