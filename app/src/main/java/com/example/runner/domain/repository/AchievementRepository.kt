package com.example.runner.domain.repository

import com.example.runner.domain.model.Achievement
import com.example.runner.domain.model.AchievementId
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    fun observeAchievements(): Flow<List<Achievement>>
    suspend fun unlock(id: AchievementId, atMillis: Long)
    suspend fun unlockedIds(): Set<AchievementId>
}
