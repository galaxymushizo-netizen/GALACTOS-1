package com.example.data

import kotlinx.coroutines.flow.Flow

class HealthRepository(private val healthDao: HealthDao) {
    val allRecords: Flow<List<HealthRecord>> = healthDao.getAllRecords()
    val allGoals: Flow<List<UserGoal>> = healthDao.getAllGoals()

    suspend fun insertRecord(record: HealthRecord) {
        healthDao.insertRecord(record)
    }

    suspend fun deleteRecord(record: HealthRecord) {
        healthDao.deleteRecord(record)
    }

    suspend fun deleteRecordById(id: Int) {
        healthDao.deleteRecordById(id)
    }

    suspend fun insertGoal(goal: UserGoal) {
        healthDao.insertGoal(goal)
    }

    suspend fun insertGoals(goals: List<UserGoal>) {
        healthDao.insertGoals(goals)
    }

    val userProfile: Flow<UserProfile?> = healthDao.getUserProfile()

    suspend fun insertProfile(profile: UserProfile) {
        healthDao.insertProfile(profile)
    }

    suspend fun deleteProfile() {
        healthDao.deleteProfile()
    }
}
