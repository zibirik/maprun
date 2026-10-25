package com.example.runner.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runner.domain.model.AppThemeMode
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.UserSettings
import com.example.runner.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings(),
    )

    fun setUnit(unit: DistanceUnit) = viewModelScope.launch { settingsRepository.setUnit(unit) }
    fun setWeight(weight: Float) = viewModelScope.launch { settingsRepository.setWeightKg(weight) }
    fun setTheme(mode: AppThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setVoiceEveryKm(km: Float) = viewModelScope.launch { settingsRepository.setVoiceEveryKm(km) }
    fun setAutoPause(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutoPause(enabled) }
}
