package com.example.runner.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runner.domain.model.PeriodStatistics
import com.example.runner.domain.util.FormatUtils
import com.example.runner.domain.util.WorkoutMath
import com.example.runner.presentation.components.SectionCard
import com.example.runner.presentation.components.SectionLabel
import com.example.runner.presentation.components.StatTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PeriodBlock("Эта неделя", state.week, state)
            PeriodBlock("Этот месяц", state.month, state)
            if (state.records.longestMeters > 0) {
                SectionCard(color = MaterialTheme.colorScheme.primaryContainer) {
                    SectionLabel("Личные рекорды")
                    Text(
                        "Самая длинная  ·  ${FormatUtils.distance(state.records.longestMeters, state.settings.unit)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (state.records.fastestPaceSecPerKm > 0) {
                        Text(
                            "Лучший темп  ·  ${FormatUtils.pace(WorkoutMath.paceForUnit(state.records.fastestPaceSecPerKm, state.settings.unit))}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    if (state.records.mostCalories > 0) {
                        Text(
                            "Больше калорий  ·  ${state.records.mostCalories} ккал",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            if (state.month.totalDistanceMeters > 0) {
                SectionCard {
                    SectionLabel("Неделя от месяца")
                    val ratio = (state.week.totalDistanceMeters / state.month.totalDistanceMeters)
                        .toFloat()
                        .coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(ratio)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Text(
                        "${(ratio * 100).toInt()}% месячной дистанции уже на этой неделе",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodBlock(title: String, stats: PeriodStatistics, state: StatsUiState) {
    val unit = state.settings.unit
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Тренировок", stats.workoutCount.toString(), Modifier.weight(1f), accent = true)
            StatTile("Дистанция", FormatUtils.distance(stats.totalDistanceMeters, unit), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Время", FormatUtils.duration(stats.totalDurationMillis), Modifier.weight(1f))
            StatTile(
                "Средний темп",
                FormatUtils.pace(WorkoutMath.paceForUnit(stats.avgPaceSecPerKm, unit)),
                Modifier.weight(1f),
            )
        }
        StatTile("Калории", "${stats.totalCalories} ккал", Modifier.fillMaxWidth())
    }
}
