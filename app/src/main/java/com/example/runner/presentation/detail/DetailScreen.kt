package com.example.runner.presentation.detail

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runner.presentation.components.EmptyState
import com.example.runner.presentation.components.HeroStat
import com.example.runner.presentation.components.MapController
import com.example.runner.presentation.components.MapLibreContainer
import com.example.runner.presentation.components.MapOverlay
import com.example.runner.presentation.components.PaceChart
import com.example.runner.presentation.components.SplitsCard
import com.example.runner.presentation.components.STYLE_DARK
import com.example.runner.presentation.components.STYLE_LIGHT
import com.example.runner.presentation.components.WorkoutStatsGrid
import com.example.runner.presentation.util.ShareHelper
import androidx.compose.material.icons.outlined.ErrorOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    darkMap: Boolean,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val controller = remember { MapController() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тренировка") },
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
        when (val ui = state) {
            DetailUiState.Loading -> {
                Column(
                    Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }
            }
            is DetailUiState.Error -> {
                BoxCentered(Modifier.padding(padding)) {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Не нашли",
                        subtitle = ui.message,
                    )
                }
            }
            is DetailUiState.Success -> {
                val unit = ui.settings.unit
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MapLibreContainer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(MaterialTheme.shapes.large),
                        styleUri = if (darkMap) STYLE_DARK else STYLE_LIGHT,
                        controller = controller,
                        overlay = MapOverlay(
                            track = ui.workout.points,
                            showStartFinish = true,
                            followUser = false,
                        ),
                    )
                    HeroStat(stats = ui.stats, unit = unit)
                    WorkoutStatsGrid(stats = ui.stats, unit = unit, compact = true)
                    PaceChart(samples = ui.stats.speedSamples, unit = unit)
                    SplitsCard(splits = ui.stats.splits)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { ShareHelper.shareGpx(context, ui.workout, ui.gpx) },
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
                                        ShareHelper.shareRouteImage(context, bitmap, ui.workout, unit)
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
                }
            }
        }
    }
}

@Composable
private fun BoxCentered(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
