package com.example.runner.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.runner.ui.theme.Ember
import kotlinx.coroutines.delay

@Composable
fun CountdownOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var step by remember { mutableIntStateOf(3) }
    val scale by animateFloatAsState(
        targetValue = if (step == 0) 1.15f else 1.05f,
        animationSpec = tween(240),
        label = "countdown-scale",
    )

    LaunchedEffect(Unit) {
        for (n in 3 downTo 1) {
            step = n
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(800)
        }
        step = 0
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(450)
        onFinished()
    }

    val label = if (step == 0) "ПОБЕЖАЛИ" else step.toString()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE0081211)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                modifier = Modifier.scale(scale),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                color = if (step == 0) Ember else Color.White,
            )
            if (step > 0) {
                Text(
                    "держись ровно",
                    modifier = Modifier.padding(top = 8.dp),
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
