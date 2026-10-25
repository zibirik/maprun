package com.example.runner.domain.util

import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.RunComparison
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object FormatUtils {
    fun duration(millis: Long): String {
        val totalSec = (millis / 1000).coerceAtLeast(0)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", m, s)
        }
    }

    fun pace(secPerUnit: Double): String {
        if (secPerUnit <= 0.0 || secPerUnit.isNaN() || secPerUnit.isInfinite() || secPerUnit > 3600) {
            return "—"
        }
        val total = secPerUnit.roundToInt()
        val m = total / 60
        val s = total % 60
        return String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    fun distance(meters: Double, unit: DistanceUnit): String {
        val value = WorkoutMath.metersToUnit(meters, unit)
        return String.format(Locale.getDefault(), "%.2f %s", value, WorkoutMath.unitLabel(unit))
    }

    fun distanceShort(meters: Double, unit: DistanceUnit): String {
        val value = WorkoutMath.metersToUnit(meters, unit)
        return String.format(Locale.getDefault(), "%.2f", value)
    }

    fun speed(speedMps: Double, unit: DistanceUnit): String {
        val value = WorkoutMath.speedForUnitKmhOrMph(speedMps, unit)
        val suffix = if (unit == DistanceUnit.KILOMETERS) "км/ч" else "миль/ч"
        return String.format(Locale.getDefault(), "%.1f %s", value, suffix)
    }

    fun elevation(meters: Double): String {
        return String.format(Locale.getDefault(), "%.0f м", meters)
    }

    fun accuracy(meters: Float?): String {
        return if (meters == null) "GPS…" else String.format(Locale.getDefault(), "±%.0f м", meters)
    }

    fun signedDuration(millis: Long): String {
        val sign = if (millis > 0) "+" else "−"
        return sign + duration(kotlin.math.abs(millis))
    }

    fun relativeAgo(timestamp: Long, now: Long = System.currentTimeMillis()): String? {
        val delta = (now - timestamp).coerceAtLeast(0)
        val minutes = delta / 60_000
        val hours = delta / 3_600_000
        return when {
            delta < 45_000 -> "только что"
            minutes < 60 -> "$minutes мин назад"
            hours < 24 -> "$hours ч назад"
            else -> null
        }
    }

    fun compareRuns(
        currentDistance: Double,
        currentPace: Double,
        lastDistance: Double,
        lastPace: Double,
        unit: DistanceUnit,
    ): RunComparison? {
        if (lastDistance < 40 && lastPace <= 0) return null
        val parts = mutableListOf<String>()
        var faster = false
        if (currentPace > 0 && lastPace > 0) {
            val delta = currentPace - lastPace
            when {
                delta < -1 -> {
                    faster = true
                    parts += "темп лучше на ${pace(kotlin.math.abs(delta))}"
                }
                delta > 1 -> parts += "темп слабее на ${pace(delta)}"
                else -> parts += "темп как в прошлый раз"
            }
        }
        val distDelta = currentDistance - lastDistance
        when {
            distDelta > 50 -> parts += "дальше на ${distance(distDelta, unit)}"
            distDelta < -50 -> parts += "короче на ${distance(-distDelta, unit)}"
        }
        if (parts.isEmpty()) return null
        return RunComparison(
            summary = parts.joinToString(" · "),
            faster = faster,
        )
    }

    fun greeting(hour: Int = LocalTime.now().hour): String {
        return when (hour) {
            in 5..10 -> "Доброе утро"
            in 11..16 -> "Добрый день"
            in 17..21 -> "Добрый вечер"
            in 22..23 -> "Поздний забег"
            else -> "Ещё ночь"
        }
    }

    fun dayLabel(timestamp: Long, now: LocalDate = LocalDate.now()): String {
        val day = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
        return when (day) {
            now -> relativeAgo(timestamp) ?: "сегодня"
            now.minusDays(1) -> "вчера"
            else -> DateTimeFormatter.ofPattern("d MMM", Locale("ru")).format(day)
        }
    }

    fun streakDays(startTimes: List<Long>, today: LocalDate = LocalDate.now()): Int {
        if (startTimes.isEmpty()) return 0
        val days = startTimes.map {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
        var cursor = today
        if (cursor !in days) cursor = cursor.minusDays(1)
        var streak = 0
        while (cursor in days) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun workoutsLabel(count: Int): String {
        val mod100 = count % 100
        val mod10 = count % 10
        val word = when {
            mod100 in 11..14 -> "тренировок"
            mod10 == 1 -> "тренировка"
            mod10 in 2..4 -> "тренировки"
            else -> "тренировок"
        }
        return "$count $word"
    }
}
