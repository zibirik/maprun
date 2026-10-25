package com.example.runner.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.WorkoutStats
import com.example.runner.domain.util.FormatUtils
import com.example.runner.domain.util.WorkoutMath

@Composable
fun HeroStat(
    stats: WorkoutStats,
    unit: DistanceUnit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            "ДИСТАНЦИЯ",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
        )
        Text(
            FormatUtils.distance(stats.distanceMeters, unit),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            "${FormatUtils.duration(stats.durationMillis)}  ·  темп ${FormatUtils.pace(WorkoutMath.paceForUnit(stats.avgPaceSecPerKm, unit))}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
        )
    }
}
