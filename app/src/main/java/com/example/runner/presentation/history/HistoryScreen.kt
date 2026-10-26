package com.example.runner.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runner.domain.usecase.GetStatisticsUseCase
import com.example.runner.domain.util.FormatUtils
import com.example.runner.domain.util.WorkoutMath
import com.example.runner.presentation.components.EmptyState
import com.example.runner.presentation.components.SectionCard
import com.example.runner.presentation.components.SectionLabel
import com.example.runner.presentation.components.TrackProgress
import com.example.runner.ui.theme.Danger
import com.example.runner.ui.theme.Success
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpen: (Long) -> Unit,
    onOpenStats: () -> Unit,
    onOpenHeatmap: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = remember { Locale("ru") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMMM, HH:mm", locale).withZone(ZoneId.systemDefault()) }
    val dayFmt = remember { DateTimeFormatter.ofPattern("d MMMM, EEEE", locale) }
    val groups = remember(state.workouts) {
        state.workouts.groupBy {
            Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    val weekBestId = remember(state.workouts, state.weekCount) {
        val weekStart = GetStatisticsUseCase.startOfWeekMillis()
        state.workouts
            .filter { it.startTime >= weekStart && it.avgPaceSecPerKm in 1.0..1_800.0 }
            .minByOrNull { it.avgPaceSecPerKm }
            ?.id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Лента") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = onOpenHeatmap) {
                        Icon(Icons.Outlined.Whatshot, contentDescription = "Тепловая карта")
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Outlined.BarChart, contentDescription = "Статистика")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.workouts.isEmpty() && !state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Outlined.DirectionsRun,
                    title = "Лента пустая",
                    subtitle = "Выйди на карту, нажми Старт — после финиша пробежка сама появится здесь.",
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    val goalMeters = state.settings.weeklyGoalKm * 1000.0
                    val ratio = if (goalMeters > 0) {
                        (state.weekDistanceMeters / goalMeters).toFloat()
                    } else {
                        0f
                    }
                    val done = ratio >= 1f
                    SectionCard(color = MaterialTheme.colorScheme.primaryContainer) {
                        SectionLabel(if (done) "Цель недели закрыта" else "Цель недели")
                        Text(
                            FormatUtils.distance(state.weekDistanceMeters, state.settings.unit),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        TrackProgress(
                            progress = ratio,
                            color = if (done) Success else MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            buildString {
                                append(FormatUtils.workoutsLabel(state.weekCount))
                                append("  ·  цель ")
                                append(FormatUtils.distance(goalMeters, state.settings.unit))
                                if (!done) {
                                    append("  ·  ещё ")
                                    append(
                                        FormatUtils.distance(
                                            (goalMeters - state.weekDistanceMeters).coerceAtLeast(0.0),
                                            state.settings.unit,
                                        ),
                                    )
                                }
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                        weekBestId?.let { id ->
                            val best = state.workouts.firstOrNull { it.id == id }
                            if (best != null) {
                                Text(
                                    "лучший темп ${FormatUtils.pace(WorkoutMath.paceForUnit(best.avgPaceSecPerKm, state.settings.unit))}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
                groups.forEach { (day, workouts) ->
                    item(key = "day-$day") {
                        DayHeader(day, dayFmt)
                    }
                    items(workouts, key = { it.id }) { workout ->
                        HistoryRow(
                            date = listOfNotNull(
                                dateFmt.format(Instant.ofEpochMilli(workout.startTime)),
                                FormatUtils.relativeAgo(workout.startTime),
                            ).joinToString(" · "),
                            distance = FormatUtils.distance(workout.distanceMeters, state.settings.unit),
                            duration = FormatUtils.duration(workout.durationMillis),
                            pace = FormatUtils.pace(
                                WorkoutMath.paceForUnit(workout.avgPaceSecPerKm, state.settings.unit),
                            ),
                            calories = "${workout.calories} ккал",
                            weekBest = workout.id == weekBestId,
                            onClick = { onOpen(workout.id) },
                            onSwipeDelete = { viewModel.askDelete(workout.id) },
                        )
                    }
                }
            }
        }
    }

    state.pendingDeleteId?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Удалить тренировку?") },
            text = { Text("Маршрут и точки будут удалены без восстановления.") },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text("Удалить", color = Danger) } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Оставить") } },
        )
    }
}

@Composable
private fun DayHeader(day: LocalDate, formatter: DateTimeFormatter) {
    val today = LocalDate.now()
    val label = when (day) {
        today -> "Сегодня"
        today.minusDays(1) -> "Вчера"
        else -> day.format(formatter)
    }
    SectionLabel(label, Modifier.padding(top = 8.dp, start = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRow(
    date: String,
    distance: String,
    duration: String,
    pace: String,
    calories: String,
    weekBest: Boolean,
    onClick: () -> Unit,
    onSwipeDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeDelete()
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(Danger.copy(alpha = 0.14f))
                    .padding(20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Danger)
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        SectionCard(
            modifier = Modifier.clickable(onClick = onClick),
            color = if (weekBest) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ) {
            Text(
                buildString {
                    append(date)
                    if (weekBest) append("  ·  лучший темп недели")
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
            Text(distance, style = MaterialTheme.typography.headlineMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(duration, style = MaterialTheme.typography.titleMedium)
                Text("темп $pace", style = MaterialTheme.typography.titleMedium)
                Text(calories, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
