package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY scheduledAt ASC")
    fun getRemindersForUser(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND status = 'ACTIVE' ORDER BY scheduledAt ASC")
    fun getActiveRemindersForUser(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedRemindersForUser(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getReminderById(id: String, userId: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET status = :status, completedAt = :completedAt WHERE id = :id AND userId = :userId")
    suspend fun updateReminderStatus(id: String, userId: String, status: String, completedAt: Long? = null)

    @Query("UPDATE reminders SET scheduledAt = :snoozeUntil, status = 'SNOOZED', snoozeUntil = :snoozeUntil WHERE id = :id AND userId = :userId")
    suspend fun snoozeReminder(id: String, userId: String, snoozeUntil: Long)

    @Query("DELETE FROM reminders WHERE id = :id AND userId = :userId")
    suspend fun deleteReminder(id: String, userId: String)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun clearAllRemindersForUser(userId: String)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY priority DESC, createdAt DESC")
    fun getGoalsForUser(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND status = 'ACTIVE' ORDER BY priority DESC, createdAt DESC")
    fun getActiveGoalsForUser(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getGoalById(id: String, userId: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET currentValue = :newValue, updatedAt = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun updateGoalProgress(id: String, userId: String, newValue: Float, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM goals WHERE id = :id AND userId = :userId")
    suspend fun deleteGoal(id: String, userId: String)

    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun clearAllGoalsForUser(userId: String)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY createdAt DESC")
    fun getHabitsForUser(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getHabitById(id: String, userId: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET currentStreak = :streak, longestStreak = :longest, lastCompletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id AND userId = :userId")
    suspend fun recordHabitCompletion(id: String, userId: String, streak: Int, longest: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM habits WHERE id = :id AND userId = :userId")
    suspend fun deleteHabit(id: String, userId: String)

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun clearAllHabitsForUser(userId: String)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines WHERE userId = :userId ORDER BY isEnabled DESC, createdAt DESC")
    fun getRoutinesForUser(userId: String): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getRoutineById(id: String, userId: String): RoutineEntity?

    @Query("SELECT * FROM routines WHERE userId = :userId AND routineType = :routineType LIMIT 1")
    suspend fun getRoutineByType(userId: String, routineType: String): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity)

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("UPDATE routines SET isEnabled = :isEnabled WHERE id = :id AND userId = :userId")
    suspend fun toggleRoutineEnabled(id: String, userId: String, isEnabled: Boolean)

    @Query("DELETE FROM routines WHERE id = :id AND userId = :userId")
    suspend fun deleteRoutine(id: String, userId: String)

    @Query("DELETE FROM routines WHERE userId = :userId")
    suspend fun clearAllRoutinesForUser(userId: String)
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAutomationsForUser(userId: String): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getAutomationById(id: String, userId: String): AutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: AutomationEntity)

    @Update
    suspend fun updateAutomation(automation: AutomationEntity)

    @Query("UPDATE automations SET status = :status WHERE id = :id AND userId = :userId")
    suspend fun updateAutomationStatus(id: String, userId: String, status: String)

    @Query("DELETE FROM automations WHERE id = :id AND userId = :userId")
    suspend fun deleteAutomation(id: String, userId: String)

    @Query("DELETE FROM automations WHERE userId = :userId")
    suspend fun clearAllAutomationsForUser(userId: String)
}

@Dao
interface ControlLogDao {
    @Query("SELECT * FROM control_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 150")
    fun getControlLogsForUser(userId: String): Flow<List<ControlLogEntity>>

    @Query("SELECT * FROM control_logs WHERE userId = :userId AND (action LIKE '%' || :query || '%' OR details LIKE '%' || :query || '%' OR source LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchControlLogs(userId: String, query: String): Flow<List<ControlLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertControlLog(log: ControlLogEntity)

    @Query("DELETE FROM control_logs WHERE id = :id AND userId = :userId")
    suspend fun deleteControlLog(id: String, userId: String)

    @Query("DELETE FROM control_logs WHERE userId = :userId")
    suspend fun clearControlLogsForUser(userId: String)
}
