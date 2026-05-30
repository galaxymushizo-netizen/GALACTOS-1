package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = HealthDatabase.getDatabase(application)
    private val repository = HealthRepository(db.healthDao())

    val allRecords = repository.allRecords
    val allGoals = repository.allGoals

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val dashboardState: StateFlow<DashboardUiState> = combine(
        repository.allRecords,
        repository.allGoals
    ) { records, goals ->
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayRecords = records.filter { it.timestamp >= midnight }

        val stepsValue = todayRecords.filter { it.type == "STEPS" }.sumOf { it.value.toInt() }
        val waterValue = todayRecords.filter { it.type == "WATER" }.sumOf { it.value.toInt() }
        val sleepValue = todayRecords.filter { it.type == "SLEEP" }.sumOf { it.value.toInt() }
        val caloriesValue = todayRecords.filter { it.type == "CALORIES" }.sumOf { it.value.toInt() }

        val lastHeartRateRecord = records.firstOrNull { it.type == "HEART_RATE" }
        val heartRateLastValue = lastHeartRateRecord?.value?.toInt() ?: 0
        val heartRateLastTimestamp = lastHeartRateRecord?.timestamp ?: 0L

        // Goal values with defaults
        val stepsGoal = goals.find { it.type == "STEPS" }?.targetValue?.toInt() ?: 10000
        val waterGoal = goals.find { it.type == "WATER" }?.targetValue?.toInt() ?: 2500
        val sleepGoal = goals.find { it.type == "SLEEP" }?.targetValue?.toInt() ?: 480
        val caloriesGoal = goals.find { it.type == "CALORIES" }?.targetValue?.toInt() ?: 2200

        DashboardUiState(
            stepsValue = stepsValue,
            stepsGoal = stepsGoal,
            waterValue = waterValue,
            waterGoal = waterGoal,
            sleepValue = sleepValue,
            sleepGoal = sleepGoal,
            caloriesValue = caloriesValue,
            caloriesGoal = caloriesGoal,
            heartRateLastValue = heartRateLastValue,
            heartRateLastTimestamp = heartRateLastTimestamp,
            recentRecords = records,
            isGoalsLoaded = goals.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun addRecord(type: String, value: Float, notes: String = "") {
        viewModelScope.launch {
            repository.insertRecord(
                HealthRecord(
                    type = type,
                    value = value,
                    notes = notes,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteRecord(record: HealthRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun deleteRecordById(id: Int) {
        viewModelScope.launch {
            repository.deleteRecordById(id)
        }
    }

    fun updateGoal(type: String, target: Float) {
        viewModelScope.launch {
            repository.insertGoal(UserGoal(type, target))
        }
    }

    fun registerProfile(username: String, name: String, focusArea: String) {
        viewModelScope.launch {
            repository.insertProfile(UserProfile(username, name, focusArea))
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.deleteProfile()
        }
    }
}

data class DashboardUiState(
    val stepsValue: Int = 0,
    val stepsGoal: Int = 10000,
    val waterValue: Int = 0,
    val waterGoal: Int = 2500,
    val sleepValue: Int = 0,
    val sleepGoal: Int = 480,
    val caloriesValue: Int = 0,
    val caloriesGoal: Int = 2200,
    val heartRateLastValue: Int = 0,
    val heartRateLastTimestamp: Long = 0,
    val recentRecords: List<HealthRecord> = emptyList(),
    val isGoalsLoaded: Boolean = false
)
