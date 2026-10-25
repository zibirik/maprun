package com.example.runner.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runner.domain.model.AppThemeMode
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.presentation.components.SectionCard
import com.example.runner.presentation.components.SectionLabel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard {
                SettingTitle(Icons.Outlined.Straighten, "Единицы")
                Text("В чём показывать дистанцию и темп", style = MaterialTheme.typography.bodyMedium)
                ChipRow {
                    FilterChip(
                        selected = settings.unit == DistanceUnit.KILOMETERS,
                        onClick = { viewModel.setUnit(DistanceUnit.KILOMETERS) },
                        label = { Text("Километры") },
                    )
                    FilterChip(
                        selected = settings.unit == DistanceUnit.MILES,
                        onClick = { viewModel.setUnit(DistanceUnit.MILES) },
                        label = { Text("Мили") },
                    )
                }
            }

            SectionCard {
                SettingTitle(Icons.Outlined.FitnessCenter, "Калории")
                Text(
                    "Вес ${String.format(Locale.getDefault(), "%.0f кг", settings.weightKg)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = settings.weightKg,
                    onValueChange = viewModel::setWeight,
                    valueRange = 40f..140f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionCard {
                SettingTitle(Icons.Outlined.DarkMode, "Тема")
                ChipRow {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        AppThemeMode.SYSTEM -> "Системная"
                                        AppThemeMode.LIGHT -> "Светлая"
                                        AppThemeMode.DARK -> "Тёмная"
                                    },
                                )
                            },
                        )
                    }
                }
            }

            SectionCard {
                SettingTitle(Icons.Outlined.RecordVoiceOver, "Голос")
                Text(
                    if (settings.voiceEveryKm <= 0f) "Подсказки выключены"
                    else "Каждые ${settings.voiceEveryKm} км",
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = settings.voiceEveryKm,
                    onValueChange = viewModel::setVoiceEveryKm,
                    valueRange = 0f..5f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Ноль выключает TextToSpeech",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            SectionCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingTitle(Icons.Outlined.PauseCircle, "Автопауза")
                        Text("Если скорость ниже 0.5 м/с дольше 10 секунд")
                    }
                    Switch(checked = settings.autoPauseEnabled, onCheckedChange = viewModel::setAutoPause)
                }
            }
            Text(
                "MapRun 1.0.0  ·  без облака, всё на телефоне\nНа пробежке экран не гаснет, чтобы карта не пропадала.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingTitle(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        SectionLabel(text)
    }
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}
