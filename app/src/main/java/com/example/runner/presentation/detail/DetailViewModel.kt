package com.example.runner.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runner.domain.model.UserSettings
import com.example.runner.domain.model.Workout
import com.example.runner.domain.model.WorkoutStats
import com.example.runner.domain.repository.SettingsRepository
import com.example.runner.domain.usecase.ExportGpxUseCase
import com.example.runner.domain.usecase.GetWorkoutDetailUseCase
import com.example.runner.domain.util.WorkoutMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Error(val message: String) : DetailUiState
    data class Success(
        val workout: Workout,
        val stats: WorkoutStats,
        val settings: UserSettings,
        val gpx: String,
    ) : DetailUiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getWorkout: GetWorkoutDetailUseCase,
    settingsRepository: SettingsRepository,
    exportGpx: ExportGpxUseCase,
) : ViewModel() {

    private val workoutId = savedStateHandle.get<String>("workoutId")?.toLongOrNull()

    val uiState = combine(
        flow {
            emit(workoutId?.let { getWorkout(it) })
        },
        settingsRepository.settings,
    ) { workout, settings ->
        when {
            workout == null -> DetailUiState.Error("Тренировка не найдена")
            else -> DetailUiState.Success(
                workout = workout,
                stats = WorkoutMath.buildStats(workout.points, workout.durationMillis, settings.weightKg),
                settings = settings,
                gpx = exportGpx(workout),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState.Loading)
}
