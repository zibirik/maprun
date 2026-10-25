package com.example.runner.domain.usecase

import com.example.runner.domain.model.TrackPoint
import com.example.runner.domain.model.Workout
import com.example.runner.domain.repository.WorkoutRepository
import com.example.runner.domain.util.GeoUtils
import javax.inject.Inject

data class GhostRun(
    val workout: Workout,
    val points: List<TrackPoint>,
)

class GetGhostRunUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(start: TrackPoint): GhostRun? {
        val candidates = repository.getAllWorkoutsWithPoints()
            .filter { it.points.size >= 8 && it.distanceMeters >= 400 }
            .mapNotNull { workout ->
                val first = workout.points.firstOrNull() ?: return@mapNotNull null
                val near = GeoUtils.haversineMeters(start, first)
                if (near <= 200.0) workout to near else null
            }
        val best = candidates.minByOrNull { (workout, _) -> workout.avgPaceSecPerKm }
            ?: return null
        return GhostRun(best.first, best.first.points)
    }
}
