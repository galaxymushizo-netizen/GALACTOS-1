package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<HealthRecord>>

    @Query("SELECT * FROM health_records WHERE type = :type ORDER BY timestamp DESC")
    fun getRecordsByType(type: String): Flow<List<HealthRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HealthRecord)

    @Delete
    suspend fun deleteRecord(record: HealthRecord)

    @Query("DELETE FROM health_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    // Goals queries
    @Query("SELECT * FROM user_goals")
    fun getAllGoals(): Flow<List<UserGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: UserGoal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<UserGoal>)

    // Profile queries
    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Query("DELETE FROM user_profiles")
    suspend fun deleteProfile()
}
