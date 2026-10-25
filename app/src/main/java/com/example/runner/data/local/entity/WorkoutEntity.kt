package com.example.runner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val distance: Double,
    val duration: Long,
    val avgPace: Double,
    val avgSpeed: Double,
    val maxSpeed: Double,
    val calories: Int,
    val elevationGain: Double,
)
