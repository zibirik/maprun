package com.example.runner.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.WorkoutStats
import com.example.runner.domain.util.FormatUtils
import com.example.runner.domain.util.WorkoutMath

@Composable
fun WorkoutStatsGrid(
    stats: WorkoutStats,
    unit: DistanceUnit,
    compact: Boolean = false,
) {
    if (!compact) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Дистанция", FormatUtils.distance(stats.distanceMeters, unit), Modifier.weight(1f), accent = true)
            StatTile("Время", FormatUtils.duration(stats.durationMillis), Modifier.weight(1f))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            "Средний темп",
            FormatUtils.pace(WorkoutMath.paceForUnit(stats.avgPaceSecPerKm, unit)),
            Modifier.weight(1f),
        )
        StatTile("Калории", "${stats.calories} ккал", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Средняя", FormatUtils.speed(stats.avgSpeedMps, unit), Modifier.weight(1f))
        StatTile("Макс.", FormatUtils.speed(stats.maxSpeedMps, unit), Modifier.weight(1f))
    }
    StatTile("Набор высоты", FormatUtils.elevation(stats.elevationGainMeters), Modifier)
}
