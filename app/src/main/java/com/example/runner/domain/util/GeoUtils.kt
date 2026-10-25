package com.example.runner.domain.util

import com.example.runner.domain.model.TrackPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_M = 6_371_000.0
    const val MAX_ACCURACY_METERS = 30f
    const val MAX_RUNNING_SPEED_MPS = 30.0 / 3.6
    const val AUTO_PAUSE_SPEED_MPS = 0.5
    const val AUTO_PAUSE_MS = 10_000L

    /** Расстояние между двумя точками по формуле гаверсинуса. */
    fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    fun haversineMeters(a: TrackPoint, b: TrackPoint): Double {
        return haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
    }

    /**
     * Отбрасываем шумные GPS-точки: плохая точность, нереалистичная скорость,
     * скачок относительно последней принятой точки.
     */
    fun isAcceptablePoint(candidate: TrackPoint, lastAccepted: TrackPoint?): Boolean {
        if (candidate.accuracyMeters > MAX_ACCURACY_METERS) return false
        if (candidate.speedMps > MAX_RUNNING_SPEED_MPS) return false
        if (lastAccepted == null) return true
        if (candidate.timestamp <= lastAccepted.timestamp) return false
        val dtSec = (candidate.timestamp - lastAccepted.timestamp) / 1000.0
        if (dtSec <= 0) return false
        val jump = haversineMeters(lastAccepted, candidate)
        val impliedSpeed = jump / dtSec
        return impliedSpeed <= MAX_RUNNING_SPEED_MPS
    }

    fun totalDistanceMeters(points: List<TrackPoint>): Double {
        if (points.size < 2) return 0.0
        return points.zipWithNext { a, b -> haversineMeters(a, b) }.sum()
    }

    /**
     * Набор высоты: суммируем только правдоподобные положительные дельты,
     * чтобы отсечь скачки GPS-альтиметра.
     */
    fun elevationGainMeters(points: List<TrackPoint>): Double {
        if (points.size < 2) return 0.0
        var gain = 0.0
        points.zipWithNext { a, b ->
            val delta = b.altitude - a.altitude
            if (delta in 0.5..10.0) {
                gain += delta
            }
        }
        return gain
    }

    fun interpolatePoint(points: List<TrackPoint>, elapsedMillis: Long): TrackPoint? {
        if (points.isEmpty()) return null
        if (elapsedMillis <= 0L) return points.first()
        val start = points.first().timestamp
        val target = start + elapsedMillis
        if (target >= points.last().timestamp) return points.last()
        val nextIndex = points.indexOfFirst { it.timestamp >= target }
        if (nextIndex <= 0) return points.first()
        val prev = points[nextIndex - 1]
        val next = points[nextIndex]
        val span = (next.timestamp - prev.timestamp).toDouble()
        if (span <= 0) return next
        val t = (target - prev.timestamp) / span
        return TrackPoint(
            latitude = prev.latitude + (next.latitude - prev.latitude) * t,
            longitude = prev.longitude + (next.longitude - prev.longitude) * t,
            timestamp = target,
            speedMps = prev.speedMps + (next.speedMps - prev.speedMps) * t,
            altitude = prev.altitude + (next.altitude - prev.altitude) * t,
            accuracyMeters = next.accuracyMeters,
        )
    }

    /** Время призрака, когда он достиг текущей дистанции бегуна. */
    fun elapsedAtDistance(points: List<TrackPoint>, targetMeters: Double): Long? {
        if (points.size < 2 || targetMeters <= 0) return null
        var acc = 0.0
        val start = points.first().timestamp
        for (i in 1 until points.size) {
            val step = haversineMeters(points[i - 1], points[i])
            if (acc + step >= targetMeters) {
                val remain = targetMeters - acc
                val ratio = if (step == 0.0) 0.0 else remain / step
                val ts = points[i - 1].timestamp +
                    ((points[i].timestamp - points[i - 1].timestamp) * ratio).toLong()
                return ts - start
            }
            acc += step
        }
        return points.last().timestamp - start
    }
}
