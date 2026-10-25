package com.example.runner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runner.domain.model.TrackingStatus
import com.example.runner.domain.model.UserSettings
import com.example.runner.domain.repository.SettingsRepository
import com.example.runner.service.TrackingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    trackingManager: TrackingManager,
) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings(),
    )

    val runActive = trackingManager.state
        .map {
            it.status == TrackingStatus.RUNNING ||
                it.status == TrackingStatus.PAUSED ||
                it.status == TrackingStatus.COUNTDOWN
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
