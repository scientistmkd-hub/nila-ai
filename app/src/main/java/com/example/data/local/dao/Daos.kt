package com.example.data.local.dao

import androidx.room.*
import com.example.core.model.MemoryCategory
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getConversationsForUser(userId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getConversationById(id: String, userId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id AND userId = :userId")
    suspend fun deleteConversation(id: String, userId: String)

    @Query("DELETE FROM conversations WHERE userId = :userId")
    suspend fun clearConversationsForUser(userId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND userId = :userId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String, userId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE userId = :userId ORDER BY timestamp DESC LIMIT 50")
    fun getRecentMessagesForUser(userId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id AND userId = :userId")
    suspend fun deleteMessage(id: String, userId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun deleteMessagesForConversation(conversationId: String, userId: String)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE userId = :userId AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getMemoriesForUser(userId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE userId = :userId AND category = :category AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getMemoriesByCategory(userId: String, category: MemoryCategory): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE userId = :userId AND isPinned = 1 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getPinnedMemoriesForUser(userId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE userId = :userId AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedMemoriesForUser(userId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE userId = :userId AND (key LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    fun searchMemoriesForUser(userId: String, query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getMemoryById(id: String, userId: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE userId = :userId")
    suspend fun getAllMemoriesList(userId: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id AND userId = :userId")
    suspend fun deleteMemory(id: String, userId: String)

    @Query("DELETE FROM memories WHERE userId = :userId")
    suspend fun clearMemoriesForUser(userId: String)
}

@Dao
interface PersonProfileDao {
    @Query("SELECT * FROM person_profiles WHERE userId = :userId AND isArchived = 0 ORDER BY lastInteraction DESC")
    fun getPersonProfilesForUser(userId: String): Flow<List<PersonProfileEntity>>

    @Query("SELECT * FROM person_profiles WHERE userId = :userId AND isArchived = 1 ORDER BY lastInteraction DESC")
    fun getArchivedPersonProfiles(userId: String): Flow<List<PersonProfileEntity>>

    @Query("SELECT * FROM person_profiles WHERE userId = :userId AND (name LIKE '%' || :query || '%' OR nickname LIKE '%' || :query || '%' OR relationship LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY lastInteraction DESC")
    fun searchPersonProfiles(userId: String, query: String): Flow<List<PersonProfileEntity>>

    @Query("SELECT * FROM person_profiles WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getPersonProfileById(id: String, userId: String): PersonProfileEntity?

    @Query("SELECT * FROM person_profiles WHERE userId = :userId AND isArchived = 0")
    suspend fun getAllPersonProfilesList(userId: String): List<PersonProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonProfile(profile: PersonProfileEntity)

    @Update
    suspend fun updatePersonProfile(profile: PersonProfileEntity)

    @Query("DELETE FROM person_profiles WHERE id = :id AND userId = :userId")
    suspend fun deletePersonProfile(id: String, userId: String)

    @Query("DELETE FROM person_profiles WHERE userId = :userId")
    suspend fun clearPersonProfilesForUser(userId: String)
}

@Dao
interface SavedObjectDao {
    @Query("SELECT * FROM saved_objects WHERE userId = :userId AND isArchived = 0 ORDER BY createdAt DESC")
    fun getSavedObjectsForUser(userId: String): Flow<List<SavedObjectEntity>>

    @Query("SELECT * FROM saved_objects WHERE userId = :userId AND isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedSavedObjects(userId: String): Flow<List<SavedObjectEntity>>

    @Query("SELECT * FROM saved_objects WHERE userId = :userId AND (name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchSavedObjects(userId: String, query: String): Flow<List<SavedObjectEntity>>

    @Query("SELECT * FROM saved_objects WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getSavedObjectById(id: String, userId: String): SavedObjectEntity?

    @Query("SELECT * FROM saved_objects WHERE userId = :userId AND isArchived = 0")
    suspend fun getAllSavedObjectsList(userId: String): List<SavedObjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedObject(obj: SavedObjectEntity)

    @Update
    suspend fun updateSavedObject(obj: SavedObjectEntity)

    @Query("DELETE FROM saved_objects WHERE id = :id AND userId = :userId")
    suspend fun deleteSavedObject(id: String, userId: String)

    @Query("DELETE FROM saved_objects WHERE userId = :userId")
    suspend fun clearSavedObjectsForUser(userId: String)
}

@Dao
interface GenerationDao {
    @Query("SELECT * FROM generation_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getGenerationsForUser(userId: String): Flow<List<GenerationHistoryEntity>>

    @Query("SELECT * FROM generation_history WHERE userId = :userId AND type = :type ORDER BY timestamp DESC")
    fun getGenerationsByType(userId: String, type: String): Flow<List<GenerationHistoryEntity>>

    @Query("SELECT * FROM generation_history WHERE userId = :userId AND isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteGenerations(userId: String): Flow<List<GenerationHistoryEntity>>

    @Query("SELECT * FROM generation_history WHERE userId = :userId AND collectionName = :collectionName ORDER BY timestamp DESC")
    fun getGenerationsByCollection(userId: String, collectionName: String): Flow<List<GenerationHistoryEntity>>

    @Query("SELECT * FROM generation_history WHERE userId = :userId AND (prompt LIKE '%' || :query || '%' OR style LIKE '%' || :query || '%' OR model LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchGenerations(userId: String, query: String): Flow<List<GenerationHistoryEntity>>

    @Query("SELECT * FROM generation_history WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getGenerationById(id: String, userId: String): GenerationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneration(generation: GenerationHistoryEntity)

    @Update
    suspend fun updateGeneration(generation: GenerationHistoryEntity)

    @Query("UPDATE generation_history SET isFavorite = :isFavorite WHERE id = :id AND userId = :userId")
    suspend fun toggleFavorite(id: String, userId: String, isFavorite: Boolean)

    @Query("UPDATE generation_history SET collectionName = :collectionName WHERE id = :id AND userId = :userId")
    suspend fun updateCollection(id: String, userId: String, collectionName: String)

    @Query("DELETE FROM generation_history WHERE id = :id AND userId = :userId")
    suspend fun deleteGeneration(id: String, userId: String)

    @Query("DELETE FROM generation_history WHERE userId = :userId")
    suspend fun clearGenerationsForUser(userId: String)
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_progress WHERE userId = :userId ORDER BY lastStudiedAt DESC")
    fun getStudyProgressForUser(userId: String): Flow<List<StudyProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyProgress(progress: StudyProgressEntity)

    @Update
    suspend fun updateStudyProgress(progress: StudyProgressEntity)

    @Query("DELETE FROM study_progress WHERE id = :id AND userId = :userId")
    suspend fun deleteStudyProgress(id: String, userId: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 0 ORDER BY isPinned DESC, isCompleted ASC, createdAt DESC")
    fun getTasksForUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, priority DESC, createdAt DESC")
    fun getPendingTasksForUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 1 AND isArchived = 0 ORDER BY completedAt DESC")
    fun getCompletedTasksForUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedTasksForUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isPinned = 1 AND isArchived = 0 ORDER BY createdAt DESC")
    fun getPinnedTasksForUser(userId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') AND isArchived = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun searchTasks(userId: String, query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getTaskById(id: String, userId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt, status = :status, updatedAt = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun toggleComplete(id: String, userId: String, isCompleted: Boolean, completedAt: Long?, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun togglePinned(id: String, userId: String, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id AND userId = :userId")
    suspend fun toggleArchived(id: String, userId: String, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM tasks WHERE id = :id AND userId = :userId")
    suspend fun deleteTask(id: String, userId: String)

    @Query("DELETE FROM tasks WHERE userId = :userId AND isCompleted = 1")
    suspend fun clearCompletedTasks(userId: String)

    @Query("DELETE FROM tasks WHERE userId = :userId")
    suspend fun clearAllTasksForUser(userId: String)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_events WHERE userId = :userId ORDER BY timestamp DESC LIMIT 100")
    fun getAuditEventsForUser(userId: String): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvent(event: AuditEventEntity)

    @Query("DELETE FROM audit_events WHERE userId = :userId")
    suspend fun clearAuditEventsForUser(userId: String)
}
