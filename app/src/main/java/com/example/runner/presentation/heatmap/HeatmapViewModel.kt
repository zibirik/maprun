package com.example.runner.presentation.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runner.domain.model.TrackPoint
import com.example.runner.domain.model.Workout
import com.example.runner.domain.repository.WorkoutRepository
import com.example.runner.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HeatmapUiState(
    val tracks: List<List<TrackPoint>> = emptyList(),
    val isLoading: Boolean = true,
    val weekOnly: Boolean = false,
    val totalRoutes: Int = 0,
)

@HiltViewModel
class HeatmapViewModel @Inject constructor(
    repository: WorkoutRepository,
) : ViewModel() {
    private val weekOnly = MutableStateFlow(false)

    val uiState = combine(
        flow { emit(repository.getAllWorkoutsWithPoints()) },
        weekOnly,
    ) { workouts, onlyWeek ->
        val weekStart = GetStatisticsUseCase.startOfWeekMillis()
        val visible = if (onlyWeek) {
            workouts.filter { it.startTime >= weekStart }
        } else {
            workouts
        }
        HeatmapUiState(
            tracks = visible.map(Workout::points),
            isLoading = false,
            weekOnly = onlyWeek,
            totalRoutes = workouts.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatmapUiState())

    fun showWeek(onlyWeek: Boolean) {
        weekOnly.value = onlyWeek
    }
}
