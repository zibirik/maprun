package com.example.runner.domain.model

data class KmSplit(
    val kmIndex: Int,
    val durationMillis: Long,
    val paceSecPerKm: Double,
)

data class SpeedSample(
    val distanceMeters: Double,
    val speedMps: Double,
    val paceSecPerKm: Double,
)

data class WorkoutStats(
    val distanceMeters: Double,
    val durationMillis: Long,
    val avgPaceSecPerKm: Double,
    val avgSpeedMps: Double,
    val maxSpeedMps: Double,
    val calories: Int,
    val elevationGainMeters: Double,
    val splits: List<KmSplit>,
    val speedSamples: List<SpeedSample>,
)

data class PeriodStatistics(
    val workoutCount: Int,
    val totalDistanceMeters: Double,
    val totalDurationMillis: Long,
    val avgPaceSecPerKm: Double,
    val totalCalories: Int,
)

data class PersonalRecords(
    val longestMeters: Double = 0.0,
    val fastestPaceSecPerKm: Double = 0.0,
    val mostCalories: Int = 0,
)

data class RunComparison(
    val summary: String,
    val faster: Boolean,
)
