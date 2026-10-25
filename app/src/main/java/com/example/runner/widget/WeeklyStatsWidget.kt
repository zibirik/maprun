package com.example.runner.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.runner.MainActivity
import com.example.runner.domain.repository.SettingsRepository
import com.example.runner.domain.repository.WorkoutRepository
import com.example.runner.domain.usecase.GetStatisticsUseCase
import com.example.runner.domain.util.FormatUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class WeeklyStatsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val weekStart = GetStatisticsUseCase.startOfWeekMillis()
        val week = entry.workoutRepository().workoutsSince(weekStart)
        val distance = week.sumOf { it.distanceMeters }
        val unit = entry.settingsRepository().settings.first().unit
        val label = FormatUtils.distance(distance, unit)
        val count = FormatUtils.workoutsLabel(week.size)

        provideContent {
            WidgetContent(context, label, count)
        }
    }
}

@Composable
private fun WidgetContent(context: Context, weeklyDistance: String, weekCount: String) {
    val startIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("runner://start")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF0C3330)))
            .padding(18.dp)
            .clickable(actionStartActivity(startIntent)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "MAPRUN · НЕДЕЛЯ",
            style = TextStyle(color = ColorProvider(Color(0xFFC7F2EC)), fontSize = 12.sp),
        )
        Text(
            text = weeklyDistance,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = weekCount,
            style = TextStyle(color = ColorProvider(Color(0xFFC7F2EC)), fontSize = 13.sp),
        )
        Spacer(GlanceModifier.height(10.dp))
        Text(
            text = "Старт пробежки  →",
            style = TextStyle(
                color = ColorProvider(Color(0xFFFF6A1A)),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

class WeeklyStatsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeeklyStatsWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun workoutRepository(): WorkoutRepository
    fun settingsRepository(): SettingsRepository
}
