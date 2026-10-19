package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val code: String = "",
    val description: String = "",
    val semester: String = "Semester 1",
    val academicYear: String = "2026",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val subjectId: String,
    val courseName: String = "",
    val chapterName: String = "",
    val title: String,
    val subtopicsJson: String = "[]",
    val masteryScore: Int = 20, // 0 to 100
    val accuracyPercent: Int = 0,
    val attemptCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val difficultyLevel: String = "MEDIUM", // EASY, MEDIUM, HARD, EXPERT
    val isWeakTopic: Boolean = false,
    val lastStudiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val subjectId: String,
    val topicId: String = "",
    val question: String,
    val answer: String,
    val hint: String = "",
    val difficulty: String = "MEDIUM",
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val reviewCount: Int = 0,
    val status: String = "LEARNING", // LEARNING, NEEDS_REVIEW, IMPROVING, STRONG
    val lastReviewedAt: Long = System.currentTimeMillis(),
    val nextReviewAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_quizzes")
data class QuizEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val subjectId: String,
    val topicTitle: String,
    val title: String,
    val totalQuestions: Int = 5,
    val timeLimitMinutes: Int = 10,
    val difficulty: String = "MEDIUM",
    val isTimed: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val userId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String, // "A", "B", "C", or "D"
    val explanation: String,
    val topicTag: String = ""
)

@Entity(tableName = "study_quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val userId: String,
    val score: Int,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val accuracyPercent: Int,
    val timeUsedSeconds: Int,
    val weakTopicsSummary: String = "",
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_goals")
data class StudyGoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String = "",
    val targetMinutes: Int = 60,
    val progressMinutes: Int = 0,
    val deadline: Long = System.currentTimeMillis() + 86400000L * 7,
    val priority: String = "HIGH", // LOW, NORMAL, HIGH, URGENT
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val fileName: String,
    val fileUri: String,
    val fileType: String = "PDF", // PDF, TXT, IMAGE
    val pageCount: Int = 1,
    val summary: String = "",
    val keyConceptsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_document_chunks")
data class DocumentChunkEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val userId: String,
    val pageNumber: Int,
    val chunkIndex: Int,
    val content: String,
    val heading: String = ""
)

@Entity(tableName = "study_coding_sessions")
data class CodingSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val language: String = "Kotlin", // Kotlin, Java, Python, JavaScript, TypeScript, C, C++, SQL
    val codeSnippet: String,
    val explanation: String,
    val analysisResult: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
