package com.example.runner.domain.usecase

import com.example.runner.domain.model.PeriodStatistics
import com.example.runner.domain.model.PersonalRecords
import com.example.runner.domain.model.Workout
import com.example.runner.domain.repository.WorkoutRepository
import com.example.runner.domain.util.WorkoutMath
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend fun week(): PeriodStatistics = periodSince(startOfWeekMillis())

    suspend fun records(): PersonalRecords {
        val workouts = repository.workoutsSince(0L)
        val paced = workouts.filter {
            it.distanceMeters >= 800 && it.avgPaceSecPerKm in 1.0..1_800.0
        }
        return PersonalRecords(
            longestMeters = workouts.maxOfOrNull { it.distanceMeters } ?: 0.0,
            fastestPaceSecPerKm = paced.minByOrNull { it.avgPaceSecPerKm }?.avgPaceSecPerKm ?: 0.0,
            mostCalories = workouts.maxOfOrNull { it.calories } ?: 0,
        )
    }

    suspend fun month(): PeriodStatistics {
        val start = LocalDate.now()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return periodSince(start)
    }

    private suspend fun periodSince(from: Long): PeriodStatistics {
        val workouts = repository.workoutsSince(from)
        return aggregate(workouts)
    }

    private fun aggregate(workouts: List<Workout>): PeriodStatistics {
        val distance = workouts.sumOf { it.distanceMeters }
        val duration = workouts.sumOf { it.durationMillis }
        return PeriodStatistics(
            workoutCount = workouts.size,
            totalDistanceMeters = distance,
            totalDurationMillis = duration,
            avgPaceSecPerKm = WorkoutMath.paceSecPerKm(duration, distance),
            totalCalories = workouts.sumOf { it.calories },
        )
    }

    companion object {
        fun startOfWeekMillis(now: LocalDate = LocalDate.now()): Long {
            return now
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }
}
