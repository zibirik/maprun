package com.example.runner.presentation.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runner.domain.model.AchievementId
import com.example.runner.domain.util.FormatUtils
import com.example.runner.presentation.components.HeroStat
import com.example.runner.presentation.components.MapController
import com.example.runner.presentation.components.MapLibreContainer
import com.example.runner.presentation.components.MapOverlay
import com.example.runner.presentation.components.PaceChart
import com.example.runner.presentation.components.STYLE_DARK
import com.example.runner.presentation.components.STYLE_LIGHT
import com.example.runner.presentation.components.SectionCard
import com.example.runner.presentation.components.SplitsCard
import com.example.runner.presentation.components.WorkoutStatsGrid
import com.example.runner.presentation.util.ShareHelper
import com.example.runner.ui.theme.Ember
import com.example.runner.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    darkMap: Boolean,
    onSaved: () -> Unit,
    onDiscarded: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val controller = remember { MapController() }
    var unlockedDialog by remember { mutableStateOf<List<AchievementId>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventsFlow.collect { event ->
            when (event) {
                is ResultEvent.Saved -> {
                    if (event.unlocked.isEmpty()) onSaved() else unlockedDialog = event.unlocked
                }
                ResultEvent.Discarded -> onDiscarded()
            }
        }
    }

    BackHandler {
        if (state.saved) viewModel.save() else viewModel.discard()
    }

    val workout = state.workout
    val stats = state.stats
    val points = workout?.points.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Финиш")
                        workout?.let {
                            Text(
                                FormatUtils.workoutWhen(it.startTime),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.saved) {
                SectionCard(color = Success.copy(alpha = 0.14f)) {
                    Text("СОХРАНЕНО", style = MaterialTheme.typography.labelLarge, color = Success)
                    Text(
                        "Пробежка уже в истории. Можно поделиться или удалить.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            if (points.size >= 2) {
                MapLibreContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(MaterialTheme.shapes.large),
                    styleUri = if (darkMap) STYLE_DARK else STYLE_LIGHT,
                    controller = controller,
                    overlay = MapOverlay(
                        track = points,
                        showStartFinish = true,
                        followUser = false,
                        userPoint = null,
                    ),
                )
            }
            if (state.tooShort) {
                SectionCard(color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)) {
                    Text("Почти без движения", style = MaterialTheme.typography.titleMedium)
                    Text("Короткая тренировка сохранится, но в статистике почти не отразится.")
                }
            }
            if (workout == null) {
                SectionCard {
                    Text("Нет данных пробежки", style = MaterialTheme.typography.titleMedium)
                    Text("Сессия уже сброшена. Открой ленту — если успели сохранить, она там.")
                }
            }
            if (workout != null && stats != null) {
                HeroStat(stats = stats, unit = state.settings.unit)
                if (state.records.isNotEmpty()) {
                    SectionCard(color = Success.copy(alpha = 0.14f)) {
                        Text("РЕКОРД", style = MaterialTheme.typography.labelLarge, color = Success)
                        state.records.forEach {
                            Text(it, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                state.comparison?.let { cmp ->
                    SectionCard(
                        color = (if (cmp.faster) Success else Ember).copy(alpha = 0.12f),
                    ) {
                        Text(
                            "С ПРОШЛОЙ ПРОБЕЖКОЙ",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (cmp.faster) Success else Ember,
                        )
                        Text(cmp.summary, style = MaterialTheme.typography.titleMedium)
                    }
                }
                WorkoutStatsGrid(stats = stats, unit = state.settings.unit, compact = true)
                PaceChart(samples = stats.speedSamples, unit = state.settings.unit)
                SplitsCard(splits = stats.splits)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { ShareHelper.shareGpx(context, workout, state.gpx) },
                        enabled = state.gpx.isNotBlank(),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Outlined.Route, contentDescription = null)
                        Text("  GPX")
                    }
                    OutlinedButton(
                        onClick = {
                            controller.snapshot { bitmap ->
                                if (bitmap != null) {
                                    ShareHelper.shareRouteImage(context, bitmap, workout, state.settings.unit)
                                } else {
                                    ShareHelper.shareSummary(context, workout, state.settings.unit)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Text("  Фото")
                    }
                }
                OutlinedButton(
                    onClick = { ShareHelper.shareSummary(context, workout, state.settings.unit) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Outlined.Sms, contentDescription = null)
                    Text("  Поделиться текстом")
                }
            }
            if (state.upcoming.isNotEmpty() && !state.saved) {
                SectionCard(color = Ember.copy(alpha = 0.12f)) {
                    Text("ОТКРОЕТСЯ ПОСЛЕ СОХРАНЕНИЯ", style = MaterialTheme.typography.labelLarge, color = Ember)
                    state.upcoming.forEach { id ->
                        Text("${id.emoji}  ${id.title}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else if (state.upcoming.isNotEmpty() && state.saved) {
                SectionCard(color = Ember.copy(alpha = 0.12f)) {
                    Text("АЧИВКИ", style = MaterialTheme.typography.labelLarge, color = Ember)
                    state.upcoming.forEach { id ->
                        Text("${id.emoji}  ${id.title}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = viewModel::save,
                enabled = !state.saving && workout != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.height(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (state.saved) "Готово" else "Сохранить в историю")
                }
            }
            OutlinedButton(
                onClick = viewModel::discard,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(if (state.saved) "Удалить тренировку" else "Не сохранять")
            }
        }
    }

    unlockedDialog?.let { ids ->
        AlertDialog(
            onDismissRequest = onSaved,
            title = { Text("Новые ачивки") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ids.forEach { Text("${it.emoji}  ${it.title}") }
                }
            },
            confirmButton = {
                TextButton(onClick = onSaved) { Text("В ленту") }
            },
        )
    }
}
