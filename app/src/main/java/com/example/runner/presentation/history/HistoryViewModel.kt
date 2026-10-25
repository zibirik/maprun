package com.example.runner.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runner.domain.model.UserSettings
import com.example.runner.domain.model.Workout
import com.example.runner.domain.repository.SettingsRepository
import com.example.runner.domain.usecase.DeleteWorkoutUseCase
import com.example.runner.domain.usecase.GetStatisticsUseCase
import com.example.runner.domain.usecase.GetWorkoutsUseCase
import com.example.runner.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val workouts: List<Workout> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val isLoading: Boolean = true,
    val pendingDeleteId: Long? = null,
    val weekDistanceMeters: Double = 0.0,
    val weekCount: Int = 0,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getWorkouts: GetWorkoutsUseCase,
    settingsRepository: SettingsRepository,
    private val deleteWorkout: DeleteWorkoutUseCase,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val pending = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)

    val uiState = combine(
        getWorkouts(),
        settingsRepository.settings,
        pending,
    ) { workouts, settings, deleteId ->
        val weekStart = GetStatisticsUseCase.startOfWeekMillis()
        val week = workouts.filter { it.startTime >= weekStart }
        HistoryUiState(
            workouts = workouts,
            settings = settings,
            isLoading = false,
            pendingDeleteId = deleteId,
            weekDistanceMeters = week.sumOf { it.distanceMeters },
            weekCount = week.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun askDelete(id: Long) {
        pending.value = id
    }

    fun cancelDelete() {
        pending.value = null
    }

    fun confirmDelete() {
        val id = pending.value ?: return
        viewModelScope.launch {
            deleteWorkout(id)
            pending.value = null
            widgetUpdater.refresh()
        }
    }
}
