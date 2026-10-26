package com.example.runner.presentation.map

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.runner.data.location.LocationClient
import com.example.runner.domain.model.DailyChallenge
import com.example.runner.domain.model.LiveTrackingState
import com.example.runner.domain.model.TrackPoint
import com.example.runner.domain.model.TrackingStatus
import com.example.runner.domain.model.UserSettings
import com.example.runner.domain.repository.SettingsRepository
import com.example.runner.domain.repository.WorkoutRepository
import com.example.runner.domain.usecase.GetDailyChallengeUseCase
import com.example.runner.domain.usecase.GetStatisticsUseCase
import com.example.runner.domain.usecase.SaveWorkoutUseCase
import com.example.runner.domain.util.FormatUtils
import com.example.runner.service.TrackingManager
import com.example.runner.service.TrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapRecap(
    val weekMeters: Double = 0.0,
    val weekCount: Int = 0,
    val streak: Int = 0,
    val lastStartTime: Long? = null,
    val lastDistance: Double = 0.0,
)

data class MapUiState(
    val tracking: LiveTrackingState = LiveTrackingState(),
    val settings: UserSettings = UserSettings(),
    val challenge: DailyChallenge? = null,
    val currentFix: TrackPoint? = null,
    val recap: MapRecap = MapRecap(),
)

sealed interface MapEvent {
    data object NavigateToResult : MapEvent
}

@HiltViewModel
class MapViewModel @Inject constructor(
    application: Application,
    private val trackingManager: TrackingManager,
    settingsRepository: SettingsRepository,
    private val getDailyChallenge: GetDailyChallengeUseCase,
    private val saveWorkout: SaveWorkoutUseCase,
    workouts: WorkoutRepository,
    locationClient: LocationClient,
) : AndroidViewModel(application) {

    private val events = Channel<MapEvent>(Channel.BUFFERED)
    val eventsFlow = events.receiveAsFlow()
    private val currentFix = MutableStateFlow<TrackPoint?>(null)
    private val challenge = MutableStateFlow<DailyChallenge?>(null)
    private val recap = MutableStateFlow(MapRecap())

    init {
        refreshChallenge()
        viewModelScope.launch {
            workouts.observeWorkouts().collect { list ->
                val weekStart = GetStatisticsUseCase.startOfWeekMillis()
                val week = list.filter { it.startTime >= weekStart }
                val last = list.maxByOrNull { it.startTime }
                recap.value = MapRecap(
                    weekMeters = week.sumOf { it.distanceMeters },
                    weekCount = week.size,
                    streak = FormatUtils.streakDays(list.map { it.startTime }),
                    lastStartTime = last?.startTime,
                    lastDistance = last?.distanceMeters ?: 0.0,
                )
            }
        }
        viewModelScope.launch {
            trackingManager.state
                .map { it.status }
                .distinctUntilChanged()
                .collect { status ->
                    if (status == TrackingStatus.FINISHED) {
                        runCatching { trackingManager.persistIfNeeded { saveWorkout(it) } }
                        events.send(MapEvent.NavigateToResult)
                    }
                    if (status == TrackingStatus.IDLE || status == TrackingStatus.FINISHED) {
                        refreshChallenge()
                    }
                }
        }
        viewModelScope.launch {
            runCatching {
                locationClient.locationUpdates(2_500L).collect { location ->
                    currentFix.value = TrackPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = location.time,
                        speedMps = location.speed.toDouble(),
                        altitude = location.altitude,
                        accuracyMeters = location.accuracy,
                    )
                }
            }
        }
    }

    val uiState: StateFlow<MapUiState> = combine(
        trackingManager.state,
        settingsRepository.settings,
        currentFix,
        challenge,
        recap,
    ) { tracking, settings, fix, daily, summary ->
        MapUiState(
            tracking = tracking,
            settings = settings,
            challenge = daily,
            currentFix = fix,
            recap = summary,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun refreshChallenge() {
        viewModelScope.launch {
            runCatching { challenge.value = getDailyChallenge() }
        }
    }

    fun startCountdown() {
        if (trackingManager.state.value.status == TrackingStatus.FINISHED) return
        trackingManager.beginCountdown()
    }

    fun onCountdownFinished() {
        sendCommand(TrackingService.ACTION_START)
    }

    fun pause() = sendCommand(TrackingService.ACTION_PAUSE)

    fun resume() = sendCommand(TrackingService.ACTION_RESUME)

    fun confirmStop() {
        if (trackingManager.state.value.status == TrackingStatus.FINISHED) {
            viewModelScope.launch { events.send(MapEvent.NavigateToResult) }
            return
        }
        trackingManager.stop()
        sendCommand(TrackingService.ACTION_STOP)
    }

    fun consumeWidgetStart() {
        if (trackingManager.state.value.status == TrackingStatus.IDLE) {
            startCountdown()
        }
    }

    private fun sendCommand(action: String) {
        val context = getApplication<Application>()
        val intent = Intent(context, TrackingService::class.java).setAction(action)
        runCatching {
            if (action == TrackingService.ACTION_START) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                // Пауза/стоп нельзя поднимать через startForegroundService:
                // на Android 12+ это убивает процесс, если сервис сразу зовёт stopSelf.
                context.startService(intent)
            }
        }
    }
}
