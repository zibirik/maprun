package com.example.runner.domain.usecase

import com.example.runner.domain.model.Workout
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportGpxUseCase @Inject constructor() {
    private val iso = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

    operator fun invoke(workout: Workout): String {
        val name = "MapRun ${iso.format(Instant.ofEpochMilli(workout.startTime))}"
        val pointsXml = workout.points.joinToString("\n") { point ->
            """
            |      <trkpt lat="${point.latitude}" lon="${point.longitude}">
            |        <ele>${point.altitude}</ele>
            |        <time>${iso.format(Instant.ofEpochMilli(point.timestamp))}</time>
            |      </trkpt>
            """.trimMargin()
        }
        return """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<gpx version="1.1" creator="MapRun" xmlns="http://www.topografix.com/GPX/1/1">
            |  <metadata>
            |    <name>$name</name>
            |    <time>${iso.format(Instant.ofEpochMilli(workout.startTime))}</time>
            |  </metadata>
            |  <trk>
            |    <name>$name</name>
            |    <trkseg>
            |$pointsXml
            |    </trkseg>
            |  </trk>
            |</gpx>
        """.trimMargin()
    }
}
