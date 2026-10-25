package com.example.runner.presentation.heatmap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.outlined.Whatshot
import com.example.runner.presentation.components.EmptyState
import com.example.runner.presentation.components.GlassChip
import com.example.runner.presentation.components.MapController
import com.example.runner.presentation.components.MapLibreContainer
import com.example.runner.presentation.components.MapOverlay
import com.example.runner.presentation.components.STYLE_DARK
import com.example.runner.presentation.components.STYLE_LIGHT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    viewModel: HeatmapViewModel,
    darkMap: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = remember { MapController() }
    val pointCount = state.tracks.sumOf { it.size }

    LaunchedEffect(state.tracks, controller.map) {
        if (!state.isLoading) {
            controller.showHeatmap(state.tracks.flatten())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тепловая карта") },
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
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            MapLibreContainer(
                modifier = Modifier.fillMaxSize(),
                styleUri = if (darkMap) STYLE_DARK else STYLE_LIGHT,
                controller = controller,
                overlay = MapOverlay(followUser = false),
                onMapReady = {
                    if (!state.isLoading) {
                        controller.showHeatmap(state.tracks.flatten())
                    }
                },
            )
            if (!state.isLoading && pointCount == 0 && state.totalRoutes == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Outlined.Whatshot,
                        title = "Карта ещё пустая",
                        subtitle = "После первой сохранённой пробежки здесь проявятся все маршруты.",
                    )
                }
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !state.weekOnly,
                    onClick = { viewModel.showWeek(false) },
                    label = { Text("Всё") },
                )
                FilterChip(
                    selected = state.weekOnly,
                    onClick = { viewModel.showWeek(true) },
                    label = { Text("Неделя") },
                )
                GlassChip {
                    Text(
                        if (state.isLoading) "Собираем маршруты…"
                        else if (pointCount == 0) {
                            if (state.weekOnly && state.totalRoutes > 0) "На этой неделе пусто"
                            else "Пока нет точек — пробегись"
                        } else {
                            "${state.tracks.size} маршрутов · $pointCount точек"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
