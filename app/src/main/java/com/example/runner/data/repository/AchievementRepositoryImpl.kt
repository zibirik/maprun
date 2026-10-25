package com.example.runner.data.repository

import com.example.runner.data.local.dao.AchievementDao
import com.example.runner.data.local.entity.AchievementEntity
import com.example.runner.domain.model.Achievement
import com.example.runner.domain.model.AchievementId
import com.example.runner.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val dao: AchievementDao,
) : AchievementRepository {

    override fun observeAchievements(): Flow<List<Achievement>> {
        return dao.observe().map { rows ->
            val unlocked = rows.associateBy { it.id }
            AchievementId.entries.map { id ->
                Achievement(
                    id = id,
                    title = id.title,
                    description = id.description,
                    isUnlocked = unlocked.containsKey(id.name),
                    unlockedAt = unlocked[id.name]?.unlockedAt,
                )
            }
        }
    }

    override suspend fun unlock(id: AchievementId, atMillis: Long) {
        dao.insert(AchievementEntity(id = id.name, unlockedAt = atMillis))
    }

    override suspend fun unlockedIds(): Set<AchievementId> {
        return dao.getAll().mapNotNull { row ->
            runCatching { AchievementId.valueOf(row.id) }.getOrNull()
        }.toSet()
    }
}
