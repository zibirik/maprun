package com.example.runner.presentation.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.model.Workout
import com.example.runner.domain.util.FormatUtils
import com.example.runner.domain.util.WorkoutMath
import java.io.File
import java.io.FileOutputStream

object ShareHelper {
    fun shareSummary(context: Context, workout: Workout, unit: DistanceUnit) {
        val text = FormatUtils.shareSummary(
            workoutDistance = workout.distanceMeters,
            durationMillis = workout.durationMillis,
            paceSecPerKm = workout.avgPaceSecPerKm,
            calories = workout.calories,
            unit = unit,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Моя пробежка")
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться"))
    }

    fun shareGpx(context: Context, workout: Workout, gpx: String) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "workout-${workout.startTime}.gpx")
        file.writeText(gpx)
        shareFile(context, file, "application/gpx+xml", "Маршрут GPX")
    }

    fun shareRouteImage(
        context: Context,
        mapBitmap: Bitmap,
        workout: Workout,
        unit: DistanceUnit,
    ) {
        val composed = composeCard(mapBitmap, workout, unit)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "workout-${workout.startTime}.png")
        FileOutputStream(file).use { composed.compress(Bitmap.CompressFormat.PNG, 95, it) }
        shareFile(context, file, "image/png", "Моя пробежка")
    }

    private fun shareFile(context: Context, file: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    private fun composeCard(map: Bitmap, workout: Workout, unit: DistanceUnit): Bitmap {
        val width = map.width.coerceAtLeast(800)
        val footer = 220
        val result = Bitmap.createBitmap(width, map.height + footer, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scaled = Bitmap.createScaledBitmap(map, width, map.height, true)
        canvas.drawBitmap(scaled, 0f, 0f, null)
        val bg = Paint().apply { color = 0xFF0F3D3A.toInt() }
        canvas.drawRect(0f, map.height.toFloat(), width.toFloat(), (map.height + footer).toFloat(), bg)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE7F3F1.toInt()
            textSize = 34f
        }
        val y0 = map.height + 64f
        canvas.drawText("MapRun", 32f, y0, titlePaint)
        val stats = listOf(
            FormatUtils.distance(workout.distanceMeters, unit),
            FormatUtils.duration(workout.durationMillis),
            FormatUtils.pace(WorkoutMath.paceForUnit(workout.avgPaceSecPerKm, unit)),
            "${workout.calories} ккал",
        ).joinToString("  ·  ")
        canvas.drawText(stats, 32f, y0 + 70f, bodyPaint)
        return result
    }
}
