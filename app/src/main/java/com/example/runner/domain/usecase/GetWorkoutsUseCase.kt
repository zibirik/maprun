package com.example.runner.domain.usecase

import com.example.runner.domain.model.Workout
import com.example.runner.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWorkoutsUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    operator fun invoke(): Flow<List<Workout>> = repository.observeWorkouts()
}

class GetWorkoutDetailUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(id: Long): Workout? = repository.getWorkout(id)
}

class DeleteWorkoutUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteWorkout(id)
}

class SaveWorkoutUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    suspend operator fun invoke(workout: com.example.runner.domain.model.Workout) =
        repository.saveWorkout(workout, workout.points)
}
