package com.example.runner.domain.usecase

import com.example.runner.domain.model.DailyChallenge
import com.example.runner.domain.model.Workout
import com.example.runner.domain.repository.SettingsRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

private data class ChallengeTemplate(
    val title: String,
    val description: String,
    val distanceMeters: Double,
    val durationMillis: Long?,
)

class GetDailyChallengeUseCase @Inject constructor(
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): DailyChallenge {
        val seed = today.toEpochDay()
        val random = Random(seed)
        val templates = listOf(
            ChallengeTemplate("Спринт на 1 км", "Пробеги 1 км быстрее 5:30", 1_000.0, 330_000L),
            ChallengeTemplate("Тройка на время", "Пробеги 3 км быстрее 18 минут", 3_000.0, 1_080_000L),
            ChallengeTemplate("Спокойные 2 км", "Закрой 2 километра сегодня", 2_000.0, null),
            ChallengeTemplate("Пятёрка", "Пробеги 5 км за одну тренировку", 5_000.0, null),
            ChallengeTemplate("Темповая пятёрка", "5 км быстрее 30 минут", 5_000.0, 1_800_000L),
        )
        val item = templates[random.nextInt(templates.size)]
        val completed = settings.lastCompletedChallengeDay() == seed
        return DailyChallenge(
            title = item.title,
            description = item.description,
            targetDistanceMeters = item.distanceMeters,
            targetDurationMillis = item.durationMillis,
            isCompleted = completed,
        )
    }

    suspend fun completeIfMatched(workout: Workout, today: LocalDate = LocalDate.now()): Boolean {
        val challenge = invoke(today)
        if (challenge.isCompleted) return false
        val distanceOk = workout.distanceMeters + 15 >= challenge.targetDistanceMeters
        val timeOk = challenge.targetDurationMillis?.let { workout.durationMillis <= it } ?: true
        return if (distanceOk && timeOk) {
            settings.markChallengeCompleted(today.toEpochDay())
            true
        } else {
            false
        }
    }
}
