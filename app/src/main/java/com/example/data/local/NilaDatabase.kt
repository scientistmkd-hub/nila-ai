package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.core.model.MemoryCategory
import com.example.core.model.MessageRole
import com.example.data.local.dao.*
import com.example.data.local.entity.*

class Converters {
    @TypeConverter
    fun fromMessageRole(role: MessageRole): String = role.name

    @TypeConverter
    fun toMessageRole(value: String): MessageRole = try {
        MessageRole.valueOf(value)
    } catch (_: Exception) {
        MessageRole.USER
    }

    @TypeConverter
    fun fromMemoryCategory(category: MemoryCategory): String = category.name

    @TypeConverter
    fun toMemoryCategory(value: String): MemoryCategory = try {
        MemoryCategory.valueOf(value)
    } catch (_: Exception) {
        MemoryCategory.PERSONAL_DETAIL
    }
}

@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        PersonProfileEntity::class,
        SavedObjectEntity::class,
        GenerationHistoryEntity::class,
        StudyProgressEntity::class,
        TaskEntity::class,
        AuditEventEntity::class,
        SubjectEntity::class,
        TopicEntity::class,
        FlashcardEntity::class,
        QuizEntity::class,
        QuizQuestionEntity::class,
        QuizAttemptEntity::class,
        StudyGoalEntity::class,
        DocumentEntity::class,
        DocumentChunkEntity::class,
        CodingSessionEntity::class,
        ReminderEntity::class,
        GoalEntity::class,
        HabitEntity::class,
        RoutineEntity::class,
        AutomationEntity::class,
        ControlLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NilaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun personProfileDao(): PersonProfileDao
    abstract fun savedObjectDao(): SavedObjectDao
    abstract fun generationDao(): GenerationDao
    abstract fun studyDao(): StudyDao
    abstract fun taskDao(): TaskDao
    abstract fun auditDao(): AuditDao
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun quizDao(): QuizDao
    abstract fun studyGoalDao(): StudyGoalDao
    abstract fun documentDao(): DocumentDao
    abstract fun codingDao(): CodingDao
    abstract fun reminderDao(): ReminderDao
    abstract fun goalDao(): GoalDao
    abstract fun habitDao(): HabitDao
    abstract fun routineDao(): RoutineDao
    abstract fun automationDao(): AutomationDao
    abstract fun controlLogDao(): ControlLogDao

    companion object {
        @Volatile
        private var INSTANCE: NilaDatabase? = null

        fun getInstance(context: Context): NilaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NilaDatabase::class.java,
                    "nila_ai_secure.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
