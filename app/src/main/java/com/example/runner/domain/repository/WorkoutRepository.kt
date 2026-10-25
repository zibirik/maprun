package com.example.runner.domain.repository

import com.example.runner.domain.model.TrackPoint
import com.example.runner.domain.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeWorkouts(): Flow<List<Workout>>
    suspend fun getWorkout(id: Long): Workout?
    suspend fun getAllWorkoutsWithPoints(): List<Workout>
    suspend fun saveWorkout(workout: Workout, points: List<TrackPoint>): Long
    suspend fun deleteWorkout(id: Long)
    suspend fun totalDistanceMeters(): Double
    suspend fun workoutsSince(startTime: Long): List<Workout>
}
