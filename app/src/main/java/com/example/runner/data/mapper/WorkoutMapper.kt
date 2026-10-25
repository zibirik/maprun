package com.example.runner.data.mapper

import com.example.runner.data.local.entity.TrackPointEntity
import com.example.runner.data.local.entity.WorkoutEntity
import com.example.runner.data.local.entity.WorkoutWithPoints
import com.example.runner.domain.model.TrackPoint
import com.example.runner.domain.model.Workout

fun WorkoutEntity.toDomain(points: List<TrackPoint> = emptyList()): Workout = Workout(
    id = id,
    startTime = startTime,
    endTime = endTime,
    distanceMeters = distance,
    durationMillis = duration,
    avgPaceSecPerKm = avgPace,
    avgSpeedMps = avgSpeed,
    maxSpeedMps = maxSpeed,
    calories = calories,
    elevationGainMeters = elevationGain,
    points = points,
)

fun WorkoutWithPoints.toDomain(): Workout = workout.toDomain(points.map { it.toDomain() })

fun TrackPointEntity.toDomain(): TrackPoint = TrackPoint(
    latitude = lat,
    longitude = lon,
    timestamp = timestamp,
    speedMps = speed,
    altitude = altitude,
    accuracyMeters = accuracy,
)

fun Workout.toEntity(): WorkoutEntity = WorkoutEntity(
    id = if (id == 0L) 0 else id,
    startTime = startTime,
    endTime = endTime,
    distance = distanceMeters,
    duration = durationMillis,
    avgPace = avgPaceSecPerKm,
    avgSpeed = avgSpeedMps,
    maxSpeed = maxSpeedMps,
    calories = calories,
    elevationGain = elevationGainMeters,
)

fun TrackPoint.toEntity(workoutId: Long): TrackPointEntity = TrackPointEntity(
    workoutId = workoutId,
    lat = latitude,
    lon = longitude,
    timestamp = timestamp,
    speed = speedMps,
    altitude = altitude,
    accuracy = accuracyMeters,
)
