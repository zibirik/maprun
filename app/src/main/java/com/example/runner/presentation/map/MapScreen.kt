package com.example.runner.presentation.map

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runner.domain.model.DailyChallenge
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.LiveTrackingState
import com.example.runner.domain.model.TrackPoint
import com.example.runner.domain.model.TrackingStatus
import com.example.runner.domain.util.FormatUtils
import com.example.runner.domain.util.WorkoutMath
import com.example.runner.presentation.components.CountdownOverlay
import com.example.runner.presentation.components.GlassChip
import com.example.runner.presentation.components.MapController
import com.example.runner.presentation.components.MapLibreContainer
import com.example.runner.presentation.components.MapOverlay
import com.example.runner.presentation.components.PulseDot
import com.example.runner.presentation.components.STYLE_DARK
import com.example.runner.presentation.components.STYLE_LIGHT
import com.example.runner.presentation.components.TopScrim
import com.example.runner.ui.theme.Danger
import com.example.runner.ui.theme.Ember
import com.example.runner.ui.theme.GhostPurple
import com.example.runner.ui.theme.Success

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    darkMap: Boolean,
    startFromWidget: Boolean,
    onConsumedWidgetStart: () -> Unit,
    onOpenHeatmap: () -> Unit,
    onFinished: () -> Unit,
    bottomLift: Dp = 0.dp,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = remember { MapController() }
    var confirmStop by remember { mutableStateOf(false) }
    val tracking = state.tracking
    val unit = state.settings.unit
    val active = tracking.status == TrackingStatus.RUNNING || tracking.status == TrackingStatus.PAUSED
    val userPoint = tracking.points.lastOrNull() ?: state.currentFix
    val ghostPoint = tracking.ghost?.let {
        TrackPoint(it.latitude, it.longitude, 0, 0.0, 0.0, 0f)
    }
    val panelLift = if (active) 0.dp else bottomLift
    var followUser by remember { mutableStateOf(true) }
    val accuracy = tracking.gpsAccuracyMeters ?: state.currentFix?.accuracyMeters
    val hasFix = tracking.hasGpsFix || state.currentFix != null
    val nextSplit = FormatUtils.distance(WorkoutMath.remainingToNextSplit(tracking.distanceMeters, unit), unit)
    val currentSplit = FormatUtils.duration(
        WorkoutMath.currentSplitMillis(tracking.points, tracking.durationMillis, unit),
    )
    val challengeProgress = state.challenge?.let {
        buildChallengeHint(it, tracking.distanceMeters, tracking.durationMillis, unit)
    }
    val avgPace = WorkoutMath.paceSecPerKm(tracking.durationMillis, tracking.distanceMeters)
    val vsAvg = when {
        tracking.currentPaceSecPerKm <= 0 || avgPace <= 0 || tracking.distanceMeters < 80 -> null
        tracking.currentPaceSecPerKm + 3 < avgPace -> "быстрее среднего" to Success
        tracking.currentPaceSecPerKm > avgPace + 8 -> "медленнее среднего" to Ember
        else -> "ровный темп" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    }
    var pauseMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(tracking.status) {
        if (tracking.status == TrackingStatus.PAUSED) {
            val started = System.currentTimeMillis()
            while (true) {
                pauseMillis = System.currentTimeMillis() - started
                delay(1_000)
            }
        } else {
            pauseMillis = 0L
        }
    }

    SideEffect {
        controller.onUserGesture = { followUser = false }
    }
    LaunchedEffect(tracking.status) {
        if (tracking.status == TrackingStatus.IDLE) followUser = true
    }
    BackHandler(enabled = active) { confirmStop = true }

    LaunchedEffect(Unit) {
        viewModel.eventsFlow.collect { event ->
            if (event is MapEvent.NavigateToResult) onFinished()
        }
    }
    LaunchedEffect(startFromWidget) {
        if (startFromWidget) {
            viewModel.consumeWidgetStart()
            onConsumedWidgetStart()
        }
    }

    Box(Modifier.fillMaxSize()) {
        MapLibreContainer(
            modifier = Modifier.fillMaxSize(),
            styleUri = if (darkMap) STYLE_DARK else STYLE_LIGHT,
            controller = controller,
            overlay = MapOverlay(
                track = tracking.points,
                ghostTrack = tracking.ghost?.ghostTrack.orEmpty(),
                ghostPoint = ghostPoint,
                userPoint = userPoint,
                followUser = followUser && tracking.status != TrackingStatus.FINISHED,
                showStartFinish = false,
            ),
        )
        TopScrim(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter),
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GpsChip(accuracy = accuracy, hasFix = hasFix)
                GlassChip {
                    IconButton(onClick = onOpenHeatmap, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Whatshot, contentDescription = "Тепловая карта")
                    }
                }
            }
            state.challenge?.let { challenge ->
                ChallengeBanner(
                    title = challenge.title,
                    description = if (active) {
                        challengeProgress?.first ?: challenge.description
                    } else {
                        challenge.description
                    },
                    done = challenge.isCompleted || (active && challengeProgress?.second == true),
                )
            }
            if (tracking.autoPaused) {
                StatusBanner("Автопауза — стоим на месте", Ember)
            }
            tracking.ghost?.let { ghost ->
                val label = if (ghost.timeDeltaMillis <= 0) {
                    "Призрак: быстрее на ${FormatUtils.duration(kotlin.math.abs(ghost.timeDeltaMillis))}"
                } else {
                    "Призрак: отстаёшь на ${FormatUtils.duration(ghost.timeDeltaMillis)}"
                }
                StatusBanner(label, GhostPurple)
            }
        }

        FloatingActionButton(
            onClick = {
                followUser = true
                userPoint?.let { controller.moveTo(it.latitude, it.longitude) }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(if (active) Modifier.navigationBarsPadding() else Modifier)
                .padding(end = 16.dp, bottom = (if (active) 292.dp else 196.dp) + panelLift),
            containerColor = if (followUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (followUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Следить за мной")
        }

        LivePanel(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(if (active) Modifier.navigationBarsPadding() else Modifier.padding(bottom = panelLift)),
            tracking = tracking,
            unitLabel = WorkoutMath.unitLabel(unit),
            distance = FormatUtils.distance(tracking.distanceMeters, unit),
            pace = FormatUtils.pace(WorkoutMath.paceForUnit(tracking.currentPaceSecPerKm, unit)),
            speed = FormatUtils.speed(tracking.currentSpeedMps, unit),
            calories = "${tracking.calories} ккал",
            elevation = FormatUtils.elevation(tracking.elevationGainMeters),
            gpsReady = hasFix,
            currentSplit = currentSplit,
            liveHint = challengeProgress?.first ?: "ещё $nextSplit до следующего км",
            greeting = FormatUtils.greeting(),
            recapLine = recapLine(state.recap, unit),
            vsAvg = vsAvg,
            pauseLabel = if (tracking.status == TrackingStatus.PAUSED && pauseMillis > 0) {
                "ПАУЗА · ${FormatUtils.duration(pauseMillis)}"
            } else {
                null
            },
            finished = tracking.status == TrackingStatus.FINISHED,
            onStart = viewModel::startCountdown,
            onPause = viewModel::pause,
            onResume = viewModel::resume,
            onStop = { confirmStop = true },
            onOpenResult = onFinished,
        )

        if (tracking.status == TrackingStatus.COUNTDOWN) {
            CountdownOverlay(onFinished = viewModel::onCountdownFinished)
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Завершить тренировку?") },
            text = { Text("Запись остановится, пробежка сохранится сама и откроется экран финиша.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    viewModel.confirmStop()
                }) { Text("Стоп", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text("Ещё побегаю") }
            },
        )
    }
}

@Composable
private fun LivePanel(
    modifier: Modifier,
    tracking: LiveTrackingState,
    unitLabel: String,
    distance: String,
    pace: String,
    speed: String,
    calories: String,
    elevation: String,
    gpsReady: Boolean,
    currentSplit: String,
    liveHint: String?,
    greeting: String,
    recapLine: String?,
    vsAvg: Pair<String, Color>?,
    pauseLabel: String?,
    finished: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onOpenResult: () -> Unit,
) {
    val active = tracking.status == TrackingStatus.RUNNING || tracking.status == TrackingStatus.PAUSED
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 20.dp,
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
            )
            Spacer(Modifier.height(14.dp))
            if (active) {
                val haptic = LocalHapticFeedback.current
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        pauseLabel ?: if (tracking.status == TrackingStatus.PAUSED) "ПАУЗА" else "В ЭФИРЕ",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (tracking.status == TrackingStatus.PAUSED) Ember else Success,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        vsAvg?.let {
                            Text(it.first, style = MaterialTheme.typography.labelLarge, color = it.second)
                        }
                        if (tracking.status == TrackingStatus.RUNNING) {
                            PulseDot(Success)
                        }
                    }
                }
                Text(
                    text = FormatUtils.duration(tracking.durationMillis),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("Дистанция", distance)
                    MiniStat("Темп", "$pace /$unitLabel")
                    MiniStat("Скорость", speed)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("Этот км", currentSplit)
                    MiniStat("Набор", elevation)
                    MiniStat("Калории", calories)
                }
                if (!liveHint.isNullOrBlank()) {
                    Text(
                        liveHint,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (tracking.status == TrackingStatus.PAUSED) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onResume()
                            },
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("  Продолжить")
                        }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPause()
                            },
                            modifier = Modifier.weight(1f).height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ember),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Text("  Пауза")
                        }
                    }
                    FilledIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStop()
                        },
                        modifier = Modifier.size(54.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Danger,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Стоп")
                    }
                }
            } else if (finished) {
                Text("Финиш", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "$distance  ·  ${FormatUtils.duration(tracking.durationMillis)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "Пробежка уже сохранена. Открой результат или вернись позже в ленту.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onOpenResult,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("К результату", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(greeting, style = MaterialTheme.typography.headlineMedium)
                recapLine?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    if (gpsReady) {
                        "Сигнал есть. Старт — и через 3 секунды поехали."
                    } else {
                        "GPS ещё ищет спутник. Можно стартовать — первые точки подтянутся сами."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                )
                Spacer(Modifier.height(16.dp))
                StartButton(onClick = onStart)
            }
        }
    }
}

@Composable
private fun StartButton(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val pulse = rememberInfiniteTransition(label = "start")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "start-scale",
    )
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(containerColor = Ember, contentColor = Color.White),
        shape = MaterialTheme.shapes.large,
    ) {
        Text("Старт", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

private fun recapLine(recap: MapRecap, unit: DistanceUnit): String? {
    if (recap.weekCount == 0 && recap.lastStartTime == null) return null
    val parts = mutableListOf<String>()
    if (recap.weekCount > 0) {
        parts += "неделя ${FormatUtils.distance(recap.weekMeters, unit)}"
    }
    if (recap.streak > 1) {
        parts += "серия ${recap.streak} дн."
    }
    recap.lastStartTime?.let { time ->
        parts += "последняя ${FormatUtils.dayLabel(time)} · ${FormatUtils.distance(recap.lastDistance, unit)}"
    }
    return parts.joinToString("  ·  ").ifBlank { null }
}

private fun buildChallengeHint(
    challenge: DailyChallenge,
    distanceMeters: Double,
    durationMillis: Long,
    unit: DistanceUnit,
): Pair<String, Boolean> {
    if (challenge.isCompleted) return "Уже закрыт сегодня" to true
    val left = (challenge.targetDistanceMeters - distanceMeters).coerceAtLeast(0.0)
    val timeOk = challenge.targetDurationMillis?.let { durationMillis <= it } ?: true
    val distanceOk = distanceMeters + 15 >= challenge.targetDistanceMeters
    return when {
        distanceOk && timeOk -> "Будет засчитан после сохранения" to true
        distanceOk && !timeOk -> "Дистанция есть, но лимит времени вышел" to false
        challenge.targetDurationMillis != null -> {
            val remain = (challenge.targetDurationMillis - durationMillis).coerceAtLeast(0)
            "ещё ${FormatUtils.distance(left, unit)} · запас ${FormatUtils.duration(remain)}" to false
        }
        else -> "ещё ${FormatUtils.distance(left, unit)}" to false
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GpsChip(accuracy: Float?, hasFix: Boolean) {
    val color = when {
        accuracy == null -> Color.Gray
        hasFix && accuracy <= 10f -> Success
        hasFix -> Ember
        else -> Danger
    }
    GlassChip {
        PulseDot(color)
        Text(FormatUtils.accuracy(accuracy), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ChallengeBanner(title: String, description: String, done: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (done) "ЧЕЛЛЕНДЖ ЗАКРЫТ" else "ЧЕЛЛЕНДЖ ДНЯ",
                style = MaterialTheme.typography.labelLarge,
                color = Ember,
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun StatusBanner(text: String, tint: Color) {
    Surface(shape = MaterialTheme.shapes.medium, color = tint) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Start,
        )
    }
}

val LOCATION_PERMISSIONS: Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()
