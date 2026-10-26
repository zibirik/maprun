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
import com.example.runner.domain.usecase.DeleteWorkoutUseCase
import com.example.runner.domain.usecase.ExportGpxUseCase
import com.example.runner.domain.usecase.GetDailyChallengeUseCase
import com.example.runner.domain.usecase.SaveWorkoutUseCase
import com.example.runner.domain.util.FormatUtils
import com.example.runner.domain.util.WorkoutMath
import com.example.runner.service.TrackingManager
import com.example.runner.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
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
    val saved: Boolean = false,
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
    private val deleteWorkout: DeleteWorkoutUseCase,
    private val checkAchievements: CheckAchievementsUseCase,
    private val dailyChallenge: GetDailyChallengeUseCase,
    private val widgetUpdater: WidgetUpdater,
    private val exportGpx: ExportGpxUseCase,
    private val workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val events = Channel<ResultEvent>(Channel.BUFFERED)
    val eventsFlow = events.receiveAsFlow()

    private val saving = MutableStateFlow(false)
    private val upcoming = MutableStateFlow<List<AchievementId>>(emptyList())
    private val error = MutableStateFlow<String?>(null)
    private val history = MutableStateFlow<List<Workout>>(emptyList())
    private val snapshot = MutableStateFlow(trackingManager.peekFinished())
    private val savedId = MutableStateFlow(trackingManager.savedWorkoutId())
    private val gpxCache = MutableStateFlow("")

    init {
        viewModelScope.launch {
            runCatching {
                val id = trackingManager.persistIfNeeded { saveWorkout(it) }
                savedId.value = id
                snapshot.value = trackingManager.peekFinished()
            }.onFailure {
                error.value = "Автосохранение не вышло — нажми «Сохранить»"
            }
            val workout = snapshot.value
            if (workout != null) {
                gpxCache.value = runCatching { exportGpx(workout) }.getOrDefault("")
                runCatching {
                    upcoming.value = checkAchievements.preview(
                        workout,
                        includeUnsaved = savedId.value == null,
                    )
                }
            }
            runCatching { history.value = workoutRepository.workoutsSince(0L) }
        }
    }

    val uiState = combine(
        combine(settingsRepository.settings, saving, upcoming, error, history) {
                settings, isSaving, soon, err, past ->
            Packed(settings, isSaving, soon, err, past)
        },
        snapshot,
        savedId,
        gpxCache,
    ) { packed, workout, id, gpx ->
        val stats = workout?.let { WorkoutMath.buildStats(it.points, it.durationMillis, packed.settings.weightKg) }
        val previous = packed.past.filter { it.id != (id ?: -1L) }
        val last = previous.maxByOrNull { it.startTime }
        val comparison = if (workout != null && last != null) {
            FormatUtils.compareRuns(
                currentDistance = workout.distanceMeters,
                currentPace = workout.avgPaceSecPerKm,
                lastDistance = last.distanceMeters,
                lastPace = last.avgPaceSecPerKm,
                unit = packed.settings.unit,
            )
        } else {
            null
        }
        val records = if (workout == null || previous.isEmpty()) {
            emptyList()
        } else {
            buildList {
                val bestDist = previous.maxOf { it.distanceMeters }
                if (workout.distanceMeters > bestDist + 5) add("Новый рекорд дистанции")
                val bestPace = previous.filter {
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
            settings = packed.settings,
            tooShort = (workout?.distanceMeters ?: 0.0) < 20.0,
            saving = packed.isSaving,
            saved = id != null,
            error = packed.err,
            upcoming = packed.soon,
            gpx = gpx,
            comparison = comparison,
            records = records,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultUiState())

    fun save() {
        viewModelScope.launch {
            saving.value = true
            val unlocked = runCatching { finalizeAndLeave() }.getOrElse {
                error.value = "Не удалось сохранить тренировку"
                emptyList()
            }
            saving.value = false
            if (error.value == null) {
                events.send(ResultEvent.Saved(unlocked))
            }
        }
    }

    fun discard() {
        viewModelScope.launch {
            runCatching {
                trackingManager.savedWorkoutId()?.let { deleteWorkout(it) }
            }
            trackingManager.discard()
            events.send(ResultEvent.Discarded)
        }
    }

    private suspend fun finalizeAndLeave(): List<AchievementId> {
        val id = trackingManager.persistIfNeeded { saveWorkout(it) }
        savedId.value = id
        val workout = trackingManager.peekFinished()?.let { current ->
            current.copy(id = id ?: current.id)
        } ?: return emptyList()
        snapshot.value = workout
        val unlocked = checkAchievements(workout)
        dailyChallenge.completeIfMatched(workout)
        widgetUpdater.refresh()
        trackingManager.discard()
        return unlocked
    }

    private data class Packed(
        val settings: UserSettings,
        val isSaving: Boolean,
        val soon: List<AchievementId>,
        val err: String?,
        val past: List<Workout>,
    )
}
