package com.example.runner.domain.util

import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.KmSplit
import com.example.runner.domain.model.SpeedSample
import com.example.runner.domain.model.TrackPoint
import com.example.runner.domain.model.WorkoutStats
import kotlin.math.max

object WorkoutMath {
    private const val METERS_PER_MILE = 1609.344
    private const val KCAL_PER_KG_PER_KM = 1.036

    fun paceSecPerKm(durationMillis: Long, distanceMeters: Double): Double {
        if (distanceMeters < 1.0) return 0.0
        val hoursEquivalentKm = distanceMeters / 1000.0
        return (durationMillis / 1000.0) / hoursEquivalentKm
    }

    fun speedMps(durationMillis: Long, distanceMeters: Double): Double {
        val sec = durationMillis / 1000.0
        if (sec <= 0) return 0.0
        return distanceMeters / sec
    }

    fun calories(weightKg: Float, distanceMeters: Double): Int {
        if (weightKg <= 0f || distanceMeters <= 0) return 0
        return (weightKg * (distanceMeters / 1000.0) * KCAL_PER_KG_PER_KM).toInt()
    }

    fun metersToUnit(meters: Double, unit: DistanceUnit): Double {
        return when (unit) {
            DistanceUnit.KILOMETERS -> meters / 1000.0
            DistanceUnit.MILES -> meters / METERS_PER_MILE
        }
    }

    fun splitLengthMeters(unit: DistanceUnit): Double {
        return when (unit) {
            DistanceUnit.KILOMETERS -> 1_000.0
            DistanceUnit.MILES -> METERS_PER_MILE
        }
    }

    /** Время текущего незакрытого километра или мили. */
    fun currentSplitMillis(
        points: List<TrackPoint>,
        durationMillis: Long,
        unit: DistanceUnit,
    ): Long {
        if (points.size < 2) return durationMillis.coerceAtLeast(0L)
        val step = splitLengthMeters(unit)
        var acc = 0.0
        var splitStart = points.first().timestamp
        for (i in 1 until points.size) {
            acc += GeoUtils.haversineMeters(points[i - 1], points[i])
            while (acc >= step) {
                acc -= step
                splitStart = points[i].timestamp
            }
        }
        return (points.last().timestamp - splitStart).coerceAtLeast(0L)
    }

    /** Сколько осталось до следующего километра или мили. */
    fun remainingToNextSplit(distanceMeters: Double, unit: DistanceUnit): Double {
        val step = splitLengthMeters(unit)
        if (distanceMeters <= 0) return step
        val rem = step - (distanceMeters % step)
        return if (rem < 0.5) step else rem
    }

    fun unitLabel(unit: DistanceUnit): String = when (unit) {
        DistanceUnit.KILOMETERS -> "км"
        DistanceUnit.MILES -> "миль"
    }

    fun paceLabel(unit: DistanceUnit): String = when (unit) {
        DistanceUnit.KILOMETERS -> "/км"
        DistanceUnit.MILES -> "/милю"
    }

    fun paceForUnit(paceSecPerKm: Double, unit: DistanceUnit): Double {
        return when (unit) {
            DistanceUnit.KILOMETERS -> paceSecPerKm
            DistanceUnit.MILES -> paceSecPerKm * (METERS_PER_MILE / 1000.0)
        }
    }

    fun speedForUnitKmhOrMph(speedMps: Double, unit: DistanceUnit): Double {
        val kmh = speedMps * 3.6
        return when (unit) {
            DistanceUnit.KILOMETERS -> kmh
            DistanceUnit.MILES -> kmh / 1.609344
        }
    }

    fun buildStats(
        points: List<TrackPoint>,
        durationMillis: Long,
        weightKg: Float,
    ): WorkoutStats {
        val distance = GeoUtils.totalDistanceMeters(points)
        val avgPace = paceSecPerKm(durationMillis, distance)
        val avgSpeed = speedMps(durationMillis, distance)
        val maxSpeed = points.maxOfOrNull { it.speedMps } ?: 0.0
        return WorkoutStats(
            distanceMeters = distance,
            durationMillis = durationMillis,
            avgPaceSecPerKm = avgPace,
            avgSpeedMps = avgSpeed,
            maxSpeedMps = maxSpeed,
            calories = calories(weightKg, distance),
            elevationGainMeters = GeoUtils.elevationGainMeters(points),
            splits = buildSplits(points),
            speedSamples = buildSamples(points),
        )
    }

    private fun buildSplits(points: List<TrackPoint>): List<KmSplit> {
        if (points.size < 2) return emptyList()
        val splits = mutableListOf<KmSplit>()
        var acc = 0.0
        var kmStartTs = points.first().timestamp
        var kmIndex = 1
        for (i in 1 until points.size) {
            acc += GeoUtils.haversineMeters(points[i - 1], points[i])
            while (acc >= 1000.0) {
                val duration = points[i].timestamp - kmStartTs
                splits += KmSplit(
                    kmIndex = kmIndex,
                    durationMillis = duration,
                    paceSecPerKm = duration / 1000.0,
                )
                acc -= 1000.0
                kmIndex += 1
                kmStartTs = points[i].timestamp
            }
        }
        return splits
    }

    private fun buildSamples(points: List<TrackPoint>): List<SpeedSample> {
        if (points.isEmpty()) return emptyList()
        val samples = mutableListOf<SpeedSample>()
        var acc = 0.0
        points.forEachIndexed { index, point ->
            if (index > 0) {
                acc += GeoUtils.haversineMeters(points[index - 1], point)
            }
            val window = points.subList(max(0, index - 4), index + 1)
            val dt = (window.last().timestamp - window.first().timestamp).coerceAtLeast(1L)
            val dd = if (window.size < 2) 0.0 else GeoUtils.totalDistanceMeters(window)
            val speed = dd / (dt / 1000.0)
            samples += SpeedSample(
                distanceMeters = acc,
                speedMps = speed,
                paceSecPerKm = if (speed <= 0.1) 0.0 else 1000.0 / speed,
            )
        }
        return downsample(samples, 80)
    }

    private fun downsample(samples: List<SpeedSample>, maxPoints: Int): List<SpeedSample> {
        if (samples.size <= maxPoints) return samples
        val step = samples.size / maxPoints.toDouble()
        return List(maxPoints) { i -> samples[(i * step).toInt().coerceAtMost(samples.lastIndex)] }
    }
}
