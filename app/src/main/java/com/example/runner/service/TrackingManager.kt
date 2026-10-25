package com.example.runner.service

import android.location.Location
import com.example.runner.domain.model.GhostProgress
import com.example.runner.domain.model.LiveTrackingState
import com.example.runner.domain.model.TrackPoint
import com.example.runner.domain.model.TrackingStatus
import com.example.runner.domain.model.Workout
import com.example.runner.domain.usecase.GhostRun
import com.example.runner.domain.util.GeoUtils
import com.example.runner.domain.util.WorkoutMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Живая сессия тренировки. Сервис пишет точки сюда, экран карты читает StateFlow.
 */
@Singleton
class TrackingManager @Inject constructor() {

    private val _state = MutableStateFlow(LiveTrackingState())
    val state: StateFlow<LiveTrackingState> = _state.asStateFlow()

    private var accumulatedMillis: Long = 0L
    private var runningSince: Long? = null
    private var lastAccepted: TrackPoint? = null
    private var lastMovingAt: Long = 0L
    private var ghostRun: GhostRun? = null
    private var weightKg: Float = 70f
    private var autoPauseEnabled: Boolean = true

    fun configure(weightKg: Float, autoPauseEnabled: Boolean) {
        this.weightKg = weightKg
        this.autoPauseEnabled = autoPauseEnabled
    }

    fun setGhost(ghost: GhostRun?) {
        ghostRun = ghost
    }

    fun beginCountdown() {
        resetClock()
        _state.value = LiveTrackingState(status = TrackingStatus.COUNTDOWN)
    }

    fun startRunning() {
        val now = System.currentTimeMillis()
        runningSince = now
        lastMovingAt = now
        _state.update {
            it.copy(
                status = TrackingStatus.RUNNING,
                startTime = if (it.startTime == 0L) now else it.startTime,
                autoPaused = false,
            )
        }
    }

    fun pause(auto: Boolean = false) {
        freezeClock()
        _state.update {
            it.copy(
                status = TrackingStatus.PAUSED,
                autoPaused = auto,
                durationMillis = accumulatedMillis,
            )
        }
    }

    fun resume() {
        val now = System.currentTimeMillis()
        runningSince = now
        lastMovingAt = now
        _state.update {
            it.copy(status = TrackingStatus.RUNNING, autoPaused = false)
        }
    }

    fun stop() {
        freezeClock()
        _state.update {
            it.copy(
                status = TrackingStatus.FINISHED,
                durationMillis = accumulatedMillis,
                autoPaused = false,
            )
        }
    }

    fun discard() {
        resetClock()
        ghostRun = null
        _state.value = LiveTrackingState()
    }

    fun tickClock() {
        val since = runningSince ?: return
        val now = System.currentTimeMillis()
        _state.update { it.copy(durationMillis = accumulatedMillis + (now - since)) }
    }

    fun onLocation(location: Location) {
        val point = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = location.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
            speedMps = location.speed.toDouble().coerceAtLeast(0.0),
            altitude = location.altitude,
            accuracyMeters = location.accuracy,
        )
        _state.update {
            it.copy(
                gpsAccuracyMeters = location.accuracy,
                hasGpsFix = location.accuracy <= GeoUtils.MAX_ACCURACY_METERS,
            )
        }

        val status = _state.value.status
        if (status != TrackingStatus.RUNNING && status != TrackingStatus.PAUSED) return

        if (!GeoUtils.isAcceptablePoint(point, lastAccepted)) return

        if (status == TrackingStatus.RUNNING) {
            handleAutoPause(point)
            if (_state.value.status != TrackingStatus.RUNNING) return
            appendPoint(point)
        } else if (_state.value.autoPaused && point.speedMps >= GeoUtils.AUTO_PAUSE_SPEED_MPS) {
            resume()
            appendPoint(point)
        }
    }

    fun finishedWorkout(): Workout {
        val live = _state.value
        val stats = WorkoutMath.buildStats(live.points, live.durationMillis, weightKg)
        return Workout(
            id = 0,
            startTime = live.startTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
            endTime = System.currentTimeMillis(),
            distanceMeters = stats.distanceMeters,
            durationMillis = stats.durationMillis,
            avgPaceSecPerKm = stats.avgPaceSecPerKm,
            avgSpeedMps = stats.avgSpeedMps,
            maxSpeedMps = stats.maxSpeedMps,
            calories = stats.calories,
            elevationGainMeters = stats.elevationGainMeters,
            points = live.points,
        )
    }

    fun markAnnouncedKm(km: Int) {
        _state.update { it.copy(lastAnnouncedKm = km) }
    }

    private fun appendPoint(point: TrackPoint) {
        val prev = lastAccepted
        lastAccepted = point
        if (point.speedMps >= GeoUtils.AUTO_PAUSE_SPEED_MPS) {
            lastMovingAt = point.timestamp
        }
        _state.update { current ->
            val points = current.points + point
            val extra = prev?.let { GeoUtils.haversineMeters(it, point) } ?: 0.0
            val distance = current.distanceMeters + extra
            val elevation = if (prev != null) {
                val delta = point.altitude - prev.altitude
                current.elevationGainMeters + if (delta in 0.5..10.0) delta else 0.0
            } else {
                current.elevationGainMeters
            }
            val duration = currentDuration()
            current.copy(
                points = points,
                distanceMeters = distance,
                durationMillis = duration,
                currentSpeedMps = point.speedMps,
                currentPaceSecPerKm = if (point.speedMps > 0.2) 1000.0 / point.speedMps else 0.0,
                calories = WorkoutMath.calories(weightKg, distance),
                elevationGainMeters = elevation,
                ghost = ghostProgress(distance, duration),
            )
        }
    }

    private fun handleAutoPause(point: TrackPoint) {
        if (!autoPauseEnabled) return
        if (point.speedMps >= GeoUtils.AUTO_PAUSE_SPEED_MPS) {
            lastMovingAt = point.timestamp
            return
        }
        if (point.timestamp - lastMovingAt >= GeoUtils.AUTO_PAUSE_MS && lastAccepted != null) {
            pause(auto = true)
        }
    }

    private fun ghostProgress(distance: Double, duration: Long): GhostProgress? {
        val ghost = ghostRun ?: return null
        val ghostPoint = GeoUtils.interpolatePoint(ghost.points, duration) ?: return null
        val ghostTime = GeoUtils.elapsedAtDistance(ghost.points, distance)
        return GhostProgress(
            latitude = ghostPoint.latitude,
            longitude = ghostPoint.longitude,
            timeDeltaMillis = duration - (ghostTime ?: ghost.workout.durationMillis),
            ghostTrack = ghost.points,
        )
    }

    private fun currentDuration(): Long {
        val since = runningSince
        return if (since == null) accumulatedMillis else accumulatedMillis + (System.currentTimeMillis() - since)
    }

    private fun freezeClock() {
        val since = runningSince
        if (since != null) {
            accumulatedMillis += System.currentTimeMillis() - since
            runningSince = null
        }
    }

    private fun resetClock() {
        accumulatedMillis = 0L
        runningSince = null
        lastAccepted = null
        lastMovingAt = 0L
    }
}
