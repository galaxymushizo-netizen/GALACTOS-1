package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_records")
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "STEPS", "WATER", "SLEEP", "CALORIES", "HEART_RATE"
    val value: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "user_goals")
data class UserGoal(
    @PrimaryKey val type: String, // "STEPS", "WATER", "SLEEP", "CALORIES"
    val targetValue: Float
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val username: String,
    val name: String,
    val focusArea: String,
    val onboardingCompleted: Boolean = true
)
