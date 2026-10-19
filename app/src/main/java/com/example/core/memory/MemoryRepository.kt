package com.example.core.memory

import android.content.Context
import android.graphics.Bitmap
import com.example.core.embedding.EmbeddingEngine
import com.example.core.model.DetectedFaceInfo
import com.example.core.model.MemoryCategory
import com.example.core.model.PersonMatchResult
import com.example.core.security.UserSessionManager
import com.example.data.local.NilaDatabase
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.PersonProfileEntity
import com.example.data.local.entity.SavedObjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MemoryRepository(context: Context) {
    private val db = NilaDatabase.getInstance(context)
    private val userSession = UserSessionManager.getInstance(context)

    val activeUserId: String
        get() = userSession.currentUser.value.id

    // ==========================================
    // 1. GENERAL MEMORY (Scoped to activeUserId)
    // ==========================================

    fun getMemories(category: MemoryCategory? = null): Flow<List<MemoryEntity>> {
        return if (category == null) {
            db.memoryDao().getMemoriesForUser(activeUserId)
        } else {
            db.memoryDao().getMemoriesByCategory(activeUserId, category)
        }
    }

    fun getPinnedMemories(): Flow<List<MemoryEntity>> {
        return db.memoryDao().getPinnedMemoriesForUser(activeUserId)
    }

    fun getArchivedMemories(): Flow<List<MemoryEntity>> {
        return db.memoryDao().getArchivedMemoriesForUser(activeUserId)
    }

    fun searchMemories(query: String): Flow<List<MemoryEntity>> {
        return db.memoryDao().searchMemoriesForUser(activeUserId, query.trim())
    }

    suspend fun saveMemory(
        title: String,
        content: String,
        category: MemoryCategory = MemoryCategory.PERSONAL_INFO,
        tags: String = "",
        importance: String = "NORMAL",
        isPinned: Boolean = false,
        source: String = "Manual"
    ): MemoryEntity = withContext(Dispatchers.IO) {
        val memory = MemoryEntity(
            id = UUID.randomUUID().toString(),
            userId = activeUserId,
            category = category,
            key = title.trim(),
            value = content.trim(),
            tags = tags.trim(),
            source = source,
            importance = importance,
            isPinned = isPinned,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.memoryDao().insertMemory(memory)
        userSession.logAuditEvent("MEMORY_SAVED", "Saved memory item: ${memory.key}")
        memory
    }

    suspend fun updateMemory(memory: MemoryEntity) = withContext(Dispatchers.IO) {
        if (memory.userId == activeUserId) {
            db.memoryDao().updateMemory(memory.copy(updatedAt = System.currentTimeMillis()))
            userSession.logAuditEvent("MEMORY_UPDATED", "Updated memory item: ${memory.key}")
        }
    }

    suspend fun deleteMemory(memoryId: String) = withContext(Dispatchers.IO) {
        db.memoryDao().deleteMemory(memoryId, activeUserId)
        userSession.logAuditEvent("MEMORY_DELETED", "Deleted memory item: $memoryId")
    }

    suspend fun clearAllMemories() = withContext(Dispatchers.IO) {
        db.memoryDao().clearMemoriesForUser(activeUserId)
        userSession.logAuditEvent("MEMORIES_CLEARED", "Cleared all personal memories for current profile")
    }

    // ==========================================
    // 2. PERSON PROFILES & PRIVATE RECOGNITION
    // ==========================================

    fun getPersonProfiles(): Flow<List<PersonProfileEntity>> {
        return db.personProfileDao().getPersonProfilesForUser(activeUserId)
    }

    fun searchPersonProfiles(query: String): Flow<List<PersonProfileEntity>> {
        return db.personProfileDao().searchPersonProfiles(activeUserId, query.trim())
    }

    suspend fun createPersonProfile(
        name: String,
        relationship: String,
        nickname: String? = null,
        department: String? = null,
        college: String? = null,
        course: String? = null,
        year: String? = null,
        isProjectPartner: Boolean = false,
        notes: String = "",
        photoUri: String? = null,
        faceInfo: DetectedFaceInfo? = null,
        photoBitmap: Bitmap? = null
    ): PersonProfileEntity = withContext(Dispatchers.IO) {
        val embeddingStr = if (faceInfo != null) {
            val vec = EmbeddingEngine.computeFaceEmbedding(faceInfo, photoBitmap)
            EmbeddingEngine.vectorToJson(vec)
        } else if (photoBitmap != null) {
            val dummyBox = android.graphics.Rect(0, 0, photoBitmap.width, photoBitmap.height)
            val dummyInfo = DetectedFaceInfo(dummyBox, null, 0f, 0f, 0.9f, 0.9f, true)
            val vec = EmbeddingEngine.computeFaceEmbedding(dummyInfo, photoBitmap)
            EmbeddingEngine.vectorToJson(vec)
        } else null

        val profile = PersonProfileEntity(
            id = UUID.randomUUID().toString(),
            userId = activeUserId,
            name = name.trim(),
            nickname = nickname?.trim()?.ifBlank { null },
            relationship = relationship.trim(),
            department = department?.trim()?.ifBlank { null },
            college = college?.trim()?.ifBlank { null },
            course = course?.trim()?.ifBlank { null },
            year = year?.trim()?.ifBlank { null },
            isProjectPartner = isProjectPartner,
            notes = notes.trim(),
            photoUri = photoUri,
            embeddingVector = embeddingStr,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.personProfileDao().insertPersonProfile(profile)
        userSession.logAuditEvent("PERSON_PROFILE_CREATED", "Saved profile for: ${profile.name}")
        profile
    }

    suspend fun updatePersonProfile(profile: PersonProfileEntity) = withContext(Dispatchers.IO) {
        if (profile.userId == activeUserId) {
            db.personProfileDao().updatePersonProfile(profile.copy(updatedAt = System.currentTimeMillis()))
            userSession.logAuditEvent("PERSON_PROFILE_UPDATED", "Updated profile: ${profile.name}")
        }
    }

    suspend fun deletePersonProfile(profileId: String) = withContext(Dispatchers.IO) {
        db.personProfileDao().deletePersonProfile(profileId, activeUserId)
        userSession.logAuditEvent("PERSON_PROFILE_DELETED", "Deleted person profile: $profileId")
    }

    /**
     * Performs face matching ONLY against current authenticated user's private profiles.
     * User B NEVER sees or matches against User A's profiles!
     */
    suspend fun matchFaceAgainstSavedProfiles(
        faceInfo: DetectedFaceInfo,
        frameBitmap: Bitmap? = null,
        threshold: Float = 0.84f
    ): PersonMatchResult? = withContext(Dispatchers.Default) {
        val savedProfiles = db.personProfileDao().getAllPersonProfilesList(activeUserId)
        if (savedProfiles.isEmpty()) return@withContext null

        val queryVec = EmbeddingEngine.computeFaceEmbedding(faceInfo, frameBitmap)

        var bestMatch: PersonProfileEntity? = null
        var highestSim = 0f

        for (profile in savedProfiles) {
            val storedVec = EmbeddingEngine.jsonToVector(profile.embeddingVector)
            if (storedVec != null) {
                val sim = EmbeddingEngine.cosineSimilarity(queryVec, storedVec)
                if (sim > highestSim) {
                    highestSim = sim
                    bestMatch = profile
                }
            }
        }

        if (bestMatch != null && highestSim >= threshold) {
            PersonMatchResult(
                matchedPersonName = bestMatch.name,
                relationship = bestMatch.relationship,
                personId = bestMatch.id,
                confidence = highestSim,
                isConfidentMatch = true,
                notes = bestMatch.notes
            )
        } else if (bestMatch != null) {
            PersonMatchResult(
                matchedPersonName = bestMatch.name,
                relationship = bestMatch.relationship,
                personId = bestMatch.id,
                confidence = highestSim,
                isConfidentMatch = false,
                notes = bestMatch.notes
            )
        } else {
            null
        }
    }

    // ==========================================
    // 3. SAVED OBJECTS
    // ==========================================

    fun getSavedObjects(): Flow<List<SavedObjectEntity>> {
        return db.savedObjectDao().getSavedObjectsForUser(activeUserId)
    }

    suspend fun saveObject(
        name: String,
        category: String,
        description: String = "",
        notes: String = "",
        location: String = "",
        tags: String = "",
        photoUri: String? = null,
        bitmap: Bitmap? = null
    ): SavedObjectEntity = withContext(Dispatchers.IO) {
        val embeddingStr = if (bitmap != null) {
            val vec = EmbeddingEngine.computeObjectEmbedding(bitmap)
            EmbeddingEngine.vectorToJson(vec)
        } else null

        val obj = SavedObjectEntity(
            id = UUID.randomUUID().toString(),
            userId = activeUserId,
            name = name.trim(),
            category = category.trim(),
            description = description.trim(),
            notes = notes.trim(),
            location = location.trim(),
            tags = tags.trim(),
            photoUri = photoUri,
            embeddingVector = embeddingStr,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.savedObjectDao().insertSavedObject(obj)
        userSession.logAuditEvent("OBJECT_SAVED", "Saved object profile: ${obj.name}")
        obj
    }

    suspend fun deleteObject(objectId: String) = withContext(Dispatchers.IO) {
        db.savedObjectDao().deleteSavedObject(objectId, activeUserId)
        userSession.logAuditEvent("OBJECT_DELETED", "Deleted object: $objectId")
    }

    // ==========================================
    // 4. MEMORY EXPORT & ACCOUNT DATA PURGE
    // ==========================================

    suspend fun exportUserDataJson(): String = withContext(Dispatchers.IO) {
        val memories = db.memoryDao().getAllMemoriesList(activeUserId)
        val profiles = db.personProfileDao().getAllPersonProfilesList(activeUserId)
        val objects = db.savedObjectDao().getAllSavedObjectsList(activeUserId)

        val root = JSONObject()
        root.put("exportVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("userId", activeUserId)

        val memArray = JSONArray()
        for (m in memories) {
            memArray.put(JSONObject().apply {
                put("id", m.id)
                put("title", m.key)
                put("content", m.value)
                put("category", m.category.name)
                put("tags", m.tags)
                put("importance", m.importance)
                put("isPinned", m.isPinned)
            })
        }
        root.put("memories", memArray)

        val profArray = JSONArray()
        for (p in profiles) {
            profArray.put(JSONObject().apply {
                put("name", p.name)
                put("relationship", p.relationship)
                put("department", p.department ?: "")
                put("college", p.college ?: "")
                put("notes", p.notes)
            })
        }
        root.put("personProfiles", profArray)

        val objArray = JSONArray()
        for (o in objects) {
            objArray.put(JSONObject().apply {
                put("name", o.name)
                put("category", o.category)
                put("description", o.description)
                put("notes", o.notes)
            })
        }
        root.put("savedObjects", objArray)

        userSession.logAuditEvent("DATA_EXPORTED", "User memory dataset exported to JSON")
        root.toString(2)
    }

    suspend fun purgeAllUserData() = withContext(Dispatchers.IO) {
        db.memoryDao().clearMemoriesForUser(activeUserId)
        db.personProfileDao().clearPersonProfilesForUser(activeUserId)
        db.savedObjectDao().clearSavedObjectsForUser(activeUserId)
        db.conversationDao().clearConversationsForUser(activeUserId)
        db.auditDao().clearAuditEventsForUser(activeUserId)
        userSession.logAuditEvent("ACCOUNT_DATA_PURGED", "All local data purged for $activeUserId")
    }
}
