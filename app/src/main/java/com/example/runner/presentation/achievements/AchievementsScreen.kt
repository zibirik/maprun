package com.example.runner.presentation.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.runner.presentation.components.SectionCard
import com.example.runner.ui.theme.Ember
import com.example.runner.ui.theme.Sand
import com.example.runner.ui.theme.TealSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Достижения") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                SectionCard(color = Sand.copy(alpha = 0.55f)) {
                    Text(
                        if (state.challengeDone) "ЧЕЛЛЕНДЖ ЗАКРЫТ" else "ЧЕЛЛЕНДЖ ДНЯ",
                        style = MaterialTheme.typography.labelLarge,
                        color = Ember,
                    )
                    Text(state.challengeTitle.ifBlank { "Загрузка…" }, style = MaterialTheme.typography.headlineMedium)
                    Text(state.challengeDescription)
                }
            }
            items(state.items, key = { it.item.id }) { item ->
                AchievementCard(item)
            }
        }
    }
}

@Composable
private fun AchievementCard(ui: AchievementUi) {
    val item = ui.item
    val color = if (item.isUnlocked) TealSoft.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
    SectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.isUnlocked) 1f else 0.82f),
        color = color,
    ) {
        Text(item.id.emoji, style = MaterialTheme.typography.headlineMedium)
        Text(
            if (item.isUnlocked) "ОТКРЫТО" else "ЕЩЁ ВПЕРЕДИ",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
        Text(item.title, style = MaterialTheme.typography.titleMedium)
        Text(
            item.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
        if (!item.isUnlocked) {
            LinearProgressIndicator(
                progress = ui.progress,
                modifier = Modifier.fillMaxWidth(),
                color = Ember,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            ui.detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
    }
}
