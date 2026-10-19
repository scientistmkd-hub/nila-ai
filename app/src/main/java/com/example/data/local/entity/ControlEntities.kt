package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val scheduledAt: Long,
    val recurrence: String = "ONCE", // ONCE, DAILY, WEEKLY, MONTHLY
    val priority: String = "NORMAL",
    val status: String = "ACTIVE", // ACTIVE, SNOOZED, COMPLETED, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val snoozeUntil: Long? = null,
    val notificationId: Int = 0
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val category: String = "Productivity",
    val targetValue: Float = 100f,
    val currentValue: Float = 0f,
    val unit: String = "hours",
    val deadline: Long? = null,
    val priority: String = "NORMAL",
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val frequency: String = "DAILY", // DAILY, WEEKLY, CUSTOM
    val target: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val routineType: String = "CUSTOM", // MORNING, STUDY, NIGHT, WORK, CUSTOM
    val triggerPhrase: String = "",
    val stepsJson: String = "[]",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val triggerType: String, // VOICE_COMMAND, TIMER_EXPIRED, TIME_OF_DAY, GESTURE
    val triggerValue: String,
    val conditionsJson: String = "[]",
    val actionsJson: String = "[]",
    val status: String = "READY", // DISABLED, READY, RUNNING, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "control_logs")
data class ControlLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val action: String,
    val source: String, // VOICE, GESTURE, BUTTON, AUTOMATION, AI
    val riskLevel: String = "LOW", // LOW, MEDIUM, HIGH, BLOCKED
    val result: String = "SUCCESS", // SUCCESS, FAILED, CANCELLED, BLOCKED
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
