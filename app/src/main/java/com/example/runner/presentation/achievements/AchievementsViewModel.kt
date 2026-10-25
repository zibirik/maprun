package com.example.runner.presentation.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runner.domain.model.Achievement
import com.example.runner.domain.model.AchievementId
import com.example.runner.domain.model.Workout
import com.example.runner.domain.repository.AchievementRepository
import com.example.runner.domain.repository.WorkoutRepository
import com.example.runner.domain.usecase.GetDailyChallengeUseCase
import com.example.runner.domain.util.FormatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AchievementUi(
    val item: Achievement,
    val progress: Float,
    val detail: String?,
)

data class AchievementsUiState(
    val items: List<AchievementUi> = emptyList(),
    val challengeTitle: String = "",
    val challengeDescription: String = "",
    val challengeDone: Boolean = false,
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    repository: AchievementRepository,
    workouts: WorkoutRepository,
    getDailyChallenge: GetDailyChallengeUseCase,
) : ViewModel() {
    val uiState = combine(
        repository.observeAchievements(),
        workouts.observeWorkouts(),
        flow { emit(getDailyChallenge()) },
    ) { items, history, challenge ->
        val total = history.sumOf { it.distanceMeters }
        val longest = history.maxOfOrNull { it.distanceMeters } ?: 0.0
        val streak = currentStreak(history)
        AchievementsUiState(
            items = items.map { achievementProgress(it, total, longest, streak) },
            challengeTitle = challenge.title,
            challengeDescription = challenge.description,
            challengeDone = challenge.isCompleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())
}

private fun achievementProgress(
    item: Achievement,
    totalMeters: Double,
    longestMeters: Double,
    streak: Int,
): AchievementUi {
    if (item.isUnlocked) {
        return AchievementUi(item, 1f, null)
    }
    val unit = com.example.runner.domain.model.DistanceUnit.KILOMETERS
    val (progress, detail) = when (item.id) {
        AchievementId.FIRST_WORKOUT -> 0f to "Сохрани первую тренировку"
        AchievementId.DISTANCE_5K -> {
            val p = (longestMeters / 5_000.0).toFloat().coerceIn(0f, 1f)
            p to "${FormatUtils.distance(longestMeters, unit)} из 5 км"
        }
        AchievementId.DISTANCE_10K -> {
            val p = (longestMeters / 10_000.0).toFloat().coerceIn(0f, 1f)
            p to "${FormatUtils.distance(longestMeters, unit)} из 10 км"
        }
        AchievementId.TOTAL_100K -> {
            val p = (totalMeters / 100_000.0).toFloat().coerceIn(0f, 1f)
            p to "${FormatUtils.distance(totalMeters, unit)} из 100 км"
        }
        AchievementId.FASTEST_KM -> {
            val p = (longestMeters / 1_000.0).toFloat().coerceIn(0f, 1f)
            p to "нужен полный километр"
        }
        AchievementId.STREAK_3 -> {
            val p = (streak / 3f).coerceIn(0f, 1f)
            p to "$streak из 3 дней"
        }
        AchievementId.STREAK_7 -> {
            val p = (streak / 7f).coerceIn(0f, 1f)
            p to "$streak из 7 дней"
        }
    }
    return AchievementUi(item, progress, detail)
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
