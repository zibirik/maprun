package com.example.runner.data.repository

import com.example.runner.data.local.dao.WorkoutDao
import com.example.runner.data.mapper.toDomain
import com.example.runner.data.mapper.toEntity
import com.example.runner.domain.model.TrackPoint
import com.example.runner.domain.model.Workout
import com.example.runner.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
) : WorkoutRepository {

    override fun observeWorkouts(): Flow<List<Workout>> {
        return workoutDao.observeWorkouts().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getWorkout(id: Long): Workout? {
        return workoutDao.getWorkoutWithPoints(id)?.toDomain()
    }

    override suspend fun getAllWorkoutsWithPoints(): List<Workout> {
        return workoutDao.getAllWithPoints().map { it.toDomain() }
    }

    override suspend fun saveWorkout(workout: Workout, points: List<TrackPoint>): Long {
        val id = workoutDao.insertWorkout(workout.toEntity())
        if (points.isNotEmpty()) {
            workoutDao.insertPoints(points.map { it.toEntity(id) })
        }
        return id
    }

    override suspend fun deleteWorkout(id: Long) {
        workoutDao.deleteWorkout(id)
    }

    override suspend fun totalDistanceMeters(): Double = workoutDao.totalDistance()

    override suspend fun workoutsSince(startTime: Long): List<Workout> {
        return workoutDao.workoutsSince(startTime).map { it.toDomain() }
    }
}
