package com.example.runner.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.runner.domain.model.KmSplit
import com.example.runner.domain.util.FormatUtils
import com.example.runner.ui.theme.Ember
import com.example.runner.ui.theme.Success

@Composable
fun SplitsCard(splits: List<KmSplit>) {
    if (splits.isEmpty()) return
    val best = splits.minByOrNull { it.paceSecPerKm }
    val worst = splits.maxByOrNull { it.paceSecPerKm }
    SectionCard {
        Text("Километровые отрезки", style = MaterialTheme.typography.titleMedium)
        val slowest = worst?.paceSecPerKm?.takeIf { it > 0 } ?: 1.0
        splits.forEach { split ->
            val color = when {
                splits.size > 1 && split == best -> Success
                splits.size > 1 && split == worst -> Ember
                else -> MaterialTheme.colorScheme.onSurface
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        buildString {
                            append("км ${split.kmIndex}")
                            if (splits.size > 1 && split == best) append("  · лучший")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = color.copy(alpha = 0.75f),
                    )
                    Text(
                        FormatUtils.pace(split.paceSecPerKm),
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                    )
                }
                TrackProgress(
                    progress = (split.paceSecPerKm / slowest).toFloat().coerceIn(0.15f, 1f),
                    color = color.copy(alpha = 0.85f),
                )
            }
        }
    }
}
