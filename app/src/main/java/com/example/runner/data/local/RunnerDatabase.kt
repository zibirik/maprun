package com.example.runner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.runner.data.local.dao.AchievementDao
import com.example.runner.data.local.dao.WorkoutDao
import com.example.runner.data.local.entity.AchievementEntity
import com.example.runner.data.local.entity.TrackPointEntity
import com.example.runner.data.local.entity.WorkoutEntity

@Database(
    entities = [
        WorkoutEntity::class,
        TrackPointEntity::class,
        AchievementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class RunnerDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun achievementDao(): AchievementDao
}
