package com.example.runner.domain.usecase

import com.example.runner.domain.model.AchievementId
import com.example.runner.domain.model.Workout
import com.example.runner.domain.repository.AchievementRepository
import com.example.runner.domain.repository.WorkoutRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class CheckAchievementsUseCase @Inject constructor(
    private val workouts: WorkoutRepository,
    private val achievements: AchievementRepository,
) {
    suspend operator fun invoke(justSaved: Workout): List<AchievementId> {
        val newly = preview(justSaved, includeUnsaved = false)
        val now = System.currentTimeMillis()
        newly.forEach { achievements.unlock(it, now) }
        return newly
    }

    /** Что откроется, если сохранить текущую тренировку. В базу не пишет. */
    suspend fun preview(candidate: Workout, includeUnsaved: Boolean = true): List<AchievementId> {
        val unlocked = achievements.unlockedIds().toMutableSet()
        val history = workouts.workoutsSince(0L).let { list ->
            if (includeUnsaved) list + candidate else list
        }
        return compute(candidate, history, unlocked)
    }

    private fun compute(
        justSaved: Workout,
        history: List<Workout>,
        already: MutableSet<AchievementId>,
    ): List<AchievementId> {
        val newly = mutableListOf<AchievementId>()
        fun unlock(id: AchievementId) {
            if (already.add(id)) newly += id
        }
        if (history.isNotEmpty()) unlock(AchievementId.FIRST_WORKOUT)
        if (justSaved.distanceMeters >= 5_000) unlock(AchievementId.DISTANCE_5K)
        if (justSaved.distanceMeters >= 10_000) unlock(AchievementId.DISTANCE_10K)
        if (history.sumOf { it.distanceMeters } >= 100_000) unlock(AchievementId.TOTAL_100K)
        if (justSaved.distanceMeters >= 1_000) unlock(AchievementId.FASTEST_KM)
        val streak = currentStreak(history)
        if (streak >= 3) unlock(AchievementId.STREAK_3)
        if (streak >= 7) unlock(AchievementId.STREAK_7)
        return newly
    }

    private fun currentStreak(history: List<Workout>): Int {
        if (history.isEmpty()) return 0
        val days = history.map {
            Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
        var cursor = LocalDate.now()
        if (cursor !in days) cursor = cursor.minusDays(1)
        var streak = 0
        while (cursor in days) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
