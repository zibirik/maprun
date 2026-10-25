package com.example.runner.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runner.domain.model.PeriodStatistics
import com.example.runner.domain.model.PersonalRecords
import com.example.runner.domain.model.UserSettings
import com.example.runner.domain.repository.SettingsRepository
import com.example.runner.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val week: PeriodStatistics = PeriodStatistics(0, 0.0, 0, 0.0, 0),
    val month: PeriodStatistics = PeriodStatistics(0, 0.0, 0, 0.0, 0),
    val settings: UserSettings = UserSettings(),
    val records: PersonalRecords = PersonalRecords(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    getStatistics: GetStatisticsUseCase,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState = combine(
        flow {
            emit(Triple(getStatistics.week(), getStatistics.month(), getStatistics.records()))
        },
        settingsRepository.settings,
    ) { periods, settings ->
        StatsUiState(
            week = periods.first,
            month = periods.second,
            settings = settings,
            records = periods.third,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
