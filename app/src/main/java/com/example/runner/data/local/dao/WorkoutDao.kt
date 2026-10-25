package com.example.runner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.runner.data.local.entity.TrackPointEntity
import com.example.runner.data.local.entity.WorkoutEntity
import com.example.runner.data.local.entity.WorkoutWithPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutWithPoints(id: Long): WorkoutWithPoints?

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    suspend fun getAllWithPoints(): List<WorkoutWithPoints>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(entity: WorkoutEntity): Long

    @Insert
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("SELECT COALESCE(SUM(distance), 0) FROM workouts")
    suspend fun totalDistance(): Double

    @Query("SELECT * FROM workouts WHERE startTime >= :from ORDER BY startTime DESC")
    suspend fun workoutsSince(from: Long): List<WorkoutEntity>
}
