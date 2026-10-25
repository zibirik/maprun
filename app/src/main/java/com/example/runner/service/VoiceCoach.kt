package com.example.runner.service

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.runner.domain.model.DistanceUnit
import com.example.runner.domain.util.FormatUtils
import com.example.runner.domain.util.WorkoutMath
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class VoiceCoach @Inject constructor(
    @ApplicationContext context: Context,
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale("ru", "RU")
            }
        }
    }

    fun speakStart() {
        speak("Побежали", "start")
    }

    fun speakFinish(distanceMeters: Double, durationMillis: Long, unit: DistanceUnit) {
        val distanceText = distancePhrase(WorkoutMath.metersToUnit(distanceMeters, unit), unit)
            .replace("Пробежали", "Финиш.")
        speak("$distanceText за ${FormatUtils.duration(durationMillis)}", "finish")
    }

    fun speakSplit(distanceMeters: Double, durationMillis: Long, unit: DistanceUnit) {
        if (!ready) return
        val distanceText = distancePhrase(WorkoutMath.metersToUnit(distanceMeters, unit), unit)
        val pace = FormatUtils.pace(
            WorkoutMath.paceForUnit(
                WorkoutMath.paceSecPerKm(durationMillis, distanceMeters),
                unit,
            ),
        )
        speak("$distanceText, темп $pace", "split-${distanceMeters.toInt()}")
    }

    private fun speak(text: String, id: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun distancePhrase(value: Double, unit: DistanceUnit): String {
        val whole = kotlin.math.abs(value - value.roundToInt()) < 0.05
        return if (whole) {
            val n = value.roundToInt()
            when (unit) {
                DistanceUnit.KILOMETERS -> "Пробежали $n ${kmWord(n)}"
                DistanceUnit.MILES -> "Пробежали $n ${mileWord(n)}"
            }
        } else {
            val pretty = formatNumber(value)
            when (unit) {
                DistanceUnit.KILOMETERS -> "Пробежали $pretty километра"
                DistanceUnit.MILES -> "Пробежали $pretty мили"
            }
        }
    }

    private fun kmWord(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        return when {
            mod100 in 11..14 -> "километров"
            mod10 == 1 -> "километр"
            mod10 in 2..4 -> "километра"
            else -> "километров"
        }
    }

    private fun mileWord(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        return when {
            mod100 in 11..14 -> "миль"
            mod10 == 1 -> "милю"
            mod10 in 2..4 -> "мили"
            else -> "миль"
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value >= 10) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value)
    }
}
