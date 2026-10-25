package com.example.runner.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.SpeedSample
import com.example.runner.domain.util.FormatUtils
import com.example.runner.ui.theme.Teal

@Composable
fun PaceChart(
    samples: List<SpeedSample>,
    title: String = "Скорость по дистанции",
    modifier: Modifier = Modifier,
    unit: DistanceUnit = DistanceUnit.KILOMETERS,
) {
    val lineColor = Teal
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    SectionCard(modifier) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        if (samples.size < 2) {
            Text(
                "Мало точек для графика — пробегите чуть дальше.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
            return@SectionCard
        }
        val peak = samples.maxOf { it.speedMps }
        Text(
            "пик ${FormatUtils.speed(peak, unit)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .padding(top = 4.dp),
        ) {
            if (samples.size < 2) return@Canvas
            val speeds = samples.map { it.speedMps.toFloat() }
            val minV = (speeds.min() - 0.4f).coerceAtLeast(0f)
            val maxV = (speeds.max() + 0.4f).coerceAtLeast(minV + 1f)
            val w = size.width
            val h = size.height
            repeat(4) { i ->
                val y = h * i / 3f
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 2f)
            }
            val line = Path()
            val fill = Path()
            samples.forEachIndexed { index, sample ->
                val x = w * index / samples.lastIndex.toFloat()
                val y = h - ((sample.speedMps.toFloat() - minV) / (maxV - minV)) * h
                if (index == 0) {
                    line.moveTo(x, y)
                    fill.moveTo(x, h)
                    fill.lineTo(x, y)
                } else {
                    line.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo(w, h)
            fill.close()
            drawPath(
                fill,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)),
                ),
            )
            drawPath(line, lineColor, style = Stroke(width = 7f, cap = StrokeCap.Round))
        }
    }
}
