package com.example.runner.presentation.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runner.domain.model.AchievementId
import com.example.runner.domain.model.RunComparison
import com.example.runner.domain.model.UserSettings
import com.example.runner.domain.model.Workout
import com.example.runner.domain.model.WorkoutStats
import com.example.runner.domain.repository.SettingsRepository
import com.example.runner.domain.repository.WorkoutRepository
import com.example.runner.domain.usecase.CheckAchievementsUseCase
import com.example.runner.domain.usecase.ExportGpxUseCase
import com.example.runner.domain.usecase.GetDailyChallengeUseCase
import com.example.runner.domain.usecase.SaveWorkoutUseCase
import com.example.runner.domain.util.FormatUtils
import com.example.runner.domain.util.WorkoutMath
import com.example.runner.service.TrackingManager
import com.example.runner.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultUiState(
    val workout: Workout? = null,
    val stats: WorkoutStats? = null,
    val settings: UserSettings = UserSettings(),
    val tooShort: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val upcoming: List<AchievementId> = emptyList(),
    val gpx: String = "",
    val comparison: RunComparison? = null,
    val records: List<String> = emptyList(),
)

sealed interface ResultEvent {
    data class Saved(val unlocked: List<AchievementId>) : ResultEvent
    data object Discarded : ResultEvent
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val trackingManager: TrackingManager,
    private val saveWorkout: SaveWorkoutUseCase,
    private val checkAchievements: CheckAchievementsUseCase,
    private val dailyChallenge: GetDailyChallengeUseCase,
    private val widgetUpdater: WidgetUpdater,
    private val exportGpx: ExportGpxUseCase,
    private val workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val events = Channel<ResultEvent>(Channel.BUFFERED)
    val eventsFlow = events.receiveAsFlow()

    private val saving = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val upcoming = kotlinx.coroutines.flow.MutableStateFlow<List<AchievementId>>(emptyList())
    private val error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val history = kotlinx.coroutines.flow.MutableStateFlow<List<Workout>>(emptyList())

    init {
        viewModelScope.launch {
            runCatching {
                upcoming.value = checkAchievements.preview(trackingManager.finishedWorkout())
            }
        }
        viewModelScope.launch {
            runCatching {
                history.value = workoutRepository.workoutsSince(0L)
            }
        }
    }

    val uiState = combine(
        combine(trackingManager.state, settingsRepository.settings, saving, upcoming, error) {
                live, settings, isSaving, soon, err ->
            Triple(live, settings, Triple(isSaving, soon, err))
        },
        history,
    ) { packed, past ->
        val live = packed.first
        val settings = packed.second
        val isSaving = packed.third.first
        val soon = packed.third.second
        val err = packed.third.third
        val workout = if (live.points.isNotEmpty() || live.durationMillis > 0) {
            trackingManager.finishedWorkout()
        } else {
            null
        }
        val stats = workout?.let { WorkoutMath.buildStats(it.points, it.durationMillis, settings.weightKg) }
        val last = past.maxByOrNull { it.startTime }
        val comparison = if (workout != null && last != null) {
            FormatUtils.compareRuns(
                currentDistance = workout.distanceMeters,
                currentPace = workout.avgPaceSecPerKm,
                lastDistance = last.distanceMeters,
                lastPace = last.avgPaceSecPerKm,
                unit = settings.unit,
            )
        } else {
            null
        }
        val records = if (workout == null || past.isEmpty()) {
            emptyList()
        } else {
            buildList {
                val bestDist = past.maxOf { it.distanceMeters }
                if (workout.distanceMeters > bestDist + 5) add("Новый рекорд дистанции")
                val bestPace = past.filter {
                    it.distanceMeters >= 800 && it.avgPaceSecPerKm in 1.0..1_800.0
                }.minOfOrNull { it.avgPaceSecPerKm }
                if (
                    bestPace != null &&
                    workout.distanceMeters >= 800 &&
                    workout.avgPaceSecPerKm in 1.0..1_800.0 &&
                    workout.avgPaceSecPerKm < bestPace - 1
                ) {
                    add("Новый рекорд темпа")
                }
            }
        }
        ResultUiState(
            workout = workout,
            stats = stats,
            settings = settings,
            tooShort = (workout?.distanceMeters ?: 0.0) < 20.0,
            saving = isSaving,
            error = err,
            upcoming = soon,
            gpx = workout?.let(exportGpx::invoke).orEmpty(),
            comparison = comparison,
            records = records,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState())

    fun save() {
        val workout = trackingManager.finishedWorkout()
        viewModelScope.launch {
            saving.value = true
            runCatching {
                val id = saveWorkout(workout)
                val saved = workout.copy(id = id)
                val fresh = checkAchievements(saved)
                dailyChallenge.completeIfMatched(saved)
                widgetUpdater.refresh()
                trackingManager.discard()
                events.send(ResultEvent.Saved(fresh))
            }.onFailure {
                error.value = "Не удалось сохранить тренировку"
            }
            saving.value = false
        }
    }

    fun discard() {
        trackingManager.discard()
        viewModelScope.launch { events.send(ResultEvent.Discarded) }
    }
}
