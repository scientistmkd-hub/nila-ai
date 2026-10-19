package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.MemoryCategory
import com.example.core.model.MessageRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String? = null,
    val displayName: String = "Guest User",
    val isGuest: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val userId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val metadata: String? = null
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: MemoryCategory,
    val key: String,
    val value: String,
    val tags: String = "",
    val source: String = "Manual",
    val importance: String = "NORMAL",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val confidence: Float = 1.0f,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "person_profiles")
data class PersonProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val nickname: String? = null,
    val relationship: String,
    val department: String? = null,
    val college: String? = null,
    val course: String? = null,
    val year: String? = null,
    val isProjectPartner: Boolean = false,
    val notes: String = "",
    val preferences: String = "",
    val photoUri: String? = null,
    val embeddingVector: String? = null,
    val isArchived: Boolean = false,
    val lastInteraction: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_objects")
data class SavedObjectEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val category: String,
    val description: String = "",
    val notes: String = "",
    val location: String = "",
    val tags: String = "",
    val photoUri: String? = null,
    val embeddingVector: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "generation_history")
data class GenerationHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val prompt: String,
    val type: String, // "IMAGE", "ANIME", "3D", "EDIT"
    val style: String = "Realistic",
    val provider: String = "Local Engine",
    val status: String = "SUCCESS", // "SUCCESS", "UNAVAILABLE", "FAILED", "CANCELLED"
    val resultInfo: String = "",
    val negativePrompt: String = "",
    val model: String = "nila-creative-v1",
    val assetUri: String = "",
    val assetType: String = "IMAGE_2D", // "IMAGE_2D", "REAL_3D_MODEL", "THREE_D_PREVIEW", "EDITED_IMAGE"
    val width: Int = 1024,
    val height: Int = 1024,
    val format: String = "", // "glb", "gltf", "obj", "png", "jpg"
    val meshUri: String? = null,
    val thumbnailUri: String? = null,
    val isFavorite: Boolean = false,
    val collectionName: String = "All Creations",
    val errorCode: String? = null,
    val metadata: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_progress")
data class StudyProgressEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val topic: String,
    val subject: String,
    val masteryPercent: Int = 0,
    val notes: String = "",
    val lastStudiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val priority: String = "NORMAL",
    val category: String = "General",
    val status: String = "TODO",
    val dueDate: Long? = null,
    val dueAt: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val recurrence: String = "NONE",
    val tags: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val eventType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
