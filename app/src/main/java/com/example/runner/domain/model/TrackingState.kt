package com.example.runner.domain.model

enum class TrackingStatus {
    IDLE,
    COUNTDOWN,
    RUNNING,
    PAUSED,
    FINISHED,
}

data class GhostProgress(
    val latitude: Double,
    val longitude: Double,
    /** Отрицательное значение — бегун быстрее призрака на текущей дистанции. */
    val timeDeltaMillis: Long,
    val ghostTrack: List<TrackPoint>,
)

data class LiveTrackingState(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val points: List<TrackPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val durationMillis: Long = 0L,
    val currentSpeedMps: Double = 0.0,
    val currentPaceSecPerKm: Double = 0.0,
    val gpsAccuracyMeters: Float? = null,
    val elevationGainMeters: Double = 0.0,
    val calories: Int = 0,
    val autoPaused: Boolean = false,
    val hasGpsFix: Boolean = false,
    val ghost: GhostProgress? = null,
    val lastAnnouncedKm: Int = 0,
    val startTime: Long = 0L,
)
