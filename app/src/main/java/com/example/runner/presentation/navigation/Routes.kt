package com.example.runner.presentation.navigation

object Routes {
    const val PERMISSIONS = "permissions"
    const val MAP = "map"
    const val RESULT = "result"
    const val HISTORY = "history"
    const val DETAIL = "detail/{workoutId}"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val HEATMAP = "heatmap"
    const val ACHIEVEMENTS = "achievements"

    fun detail(id: Long) = "detail/$id"
}

enum class BottomTab(val route: String, val label: String) {
    MAP(Routes.MAP, "Карта"),
    HISTORY(Routes.HISTORY, "История"),
    ACHIEVEMENTS(Routes.ACHIEVEMENTS, "Ачивки"),
    SETTINGS(Routes.SETTINGS, "Ещё"),
}
