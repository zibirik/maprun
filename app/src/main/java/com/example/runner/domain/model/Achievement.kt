package com.example.runner.domain.model

enum class AchievementId {
    FIRST_WORKOUT,
    DISTANCE_5K,
    DISTANCE_10K,
    TOTAL_100K,
    FASTEST_KM,
    STREAK_3,
    STREAK_7,
    ;

    val title: String
        get() = when (this) {
            FIRST_WORKOUT -> "Первый шаг"
            DISTANCE_5K -> "Пятёрка"
            DISTANCE_10K -> "Десятка"
            TOTAL_100K -> "Сотня"
            FASTEST_KM -> "Быстрый километр"
            STREAK_3 -> "Три дня подряд"
            STREAK_7 -> "Неделя огня"
        }

    val description: String
        get() = when (this) {
            FIRST_WORKOUT -> "Завершите первую тренировку"
            DISTANCE_5K -> "Пробегите 5 км за одну тренировку"
            DISTANCE_10K -> "Пробегите 10 км за одну тренировку"
            TOTAL_100K -> "Наберите 100 км суммарно"
            FASTEST_KM -> "Закройте хотя бы один полный километр"
            STREAK_3 -> "Тренируйтесь 3 дня подряд"
            STREAK_7 -> "Тренируйтесь 7 дней подряд"
        }

    val emoji: String
        get() = when (this) {
            FIRST_WORKOUT -> "👟"
            DISTANCE_5K -> "5️⃣"
            DISTANCE_10K -> "🔟"
            TOTAL_100K -> "💯"
            FASTEST_KM -> "⚡"
            STREAK_3 -> "🔥"
            STREAK_7 -> "🗓️"
        }
}

data class Achievement(
    val id: AchievementId,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null,
)

data class DailyChallenge(
    val title: String,
    val description: String,
    val targetDistanceMeters: Double,
    val targetDurationMillis: Long?,
    val isCompleted: Boolean,
)
