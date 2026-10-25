package com.example.runner.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutWithPoints(
    @Embedded val workout: WorkoutEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutId")
    val points: List<TrackPointEntity>,
)
