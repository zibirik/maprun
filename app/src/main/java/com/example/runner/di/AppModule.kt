package com.example.runner.di

import android.content.Context
import androidx.room.Room
import com.example.runner.data.local.RunnerDatabase
import com.example.runner.data.local.dao.AchievementDao
import com.example.runner.data.local.dao.WorkoutDao
import com.example.runner.data.preferences.SettingsDataStore
import com.example.runner.data.repository.AchievementRepositoryImpl
import com.example.runner.data.repository.WorkoutRepositoryImpl
import com.example.runner.domain.repository.AchievementRepository
import com.example.runner.domain.repository.SettingsRepository
import com.example.runner.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RunnerDatabase {
        return Room.databaseBuilder(context, RunnerDatabase::class.java, "maprun.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideWorkoutDao(db: RunnerDatabase): WorkoutDao = db.workoutDao()

    @Provides
    fun provideAchievementDao(db: RunnerDatabase): AchievementDao = db.achievementDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository
}
