package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM study_subjects WHERE userId = :userId ORDER BY isPinned DESC, updatedAt DESC")
    fun getSubjectsForUser(userId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM study_subjects WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getSubjectById(id: String, userId: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE study_subjects SET isPinned = :isPinned WHERE id = :id AND userId = :userId")
    suspend fun setPinned(id: String, userId: String, isPinned: Boolean)

    @Query("DELETE FROM study_subjects WHERE id = :id AND userId = :userId")
    suspend fun deleteSubject(id: String, userId: String)

    @Query("DELETE FROM study_subjects WHERE userId = :userId")
    suspend fun clearSubjectsForUser(userId: String)
}

@Dao
interface TopicDao {
    @Query("SELECT * FROM study_topics WHERE userId = :userId AND subjectId = :subjectId ORDER BY chapterName ASC, title ASC")
    fun getTopicsForSubject(userId: String, subjectId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM study_topics WHERE userId = :userId ORDER BY lastStudiedAt DESC")
    fun getAllTopicsForUser(userId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM study_topics WHERE userId = :userId AND (isWeakTopic = 1 OR masteryScore < 50) ORDER BY masteryScore ASC")
    fun getWeakTopicsForUser(userId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM study_topics WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getTopicById(id: String, userId: String): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("DELETE FROM study_topics WHERE id = :id AND userId = :userId")
    suspend fun deleteTopic(id: String, userId: String)

    @Query("DELETE FROM study_topics WHERE userId = :userId")
    suspend fun clearTopicsForUser(userId: String)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM study_flashcards WHERE userId = :userId ORDER BY nextReviewAt ASC")
    fun getAllFlashcardsForUser(userId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM study_flashcards WHERE userId = :userId AND subjectId = :subjectId ORDER BY nextReviewAt ASC")
    fun getFlashcardsForSubject(userId: String, subjectId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM study_flashcards WHERE userId = :userId AND nextReviewAt <= :currentTime ORDER BY nextReviewAt ASC")
    fun getDueFlashcards(userId: String, currentTime: Long): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Query("DELETE FROM study_flashcards WHERE id = :id AND userId = :userId")
    suspend fun deleteFlashcard(id: String, userId: String)

    @Query("DELETE FROM study_flashcards WHERE userId = :userId")
    suspend fun clearFlashcardsForUser(userId: String)
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM study_quizzes WHERE userId = :userId ORDER BY createdAt DESC")
    fun getQuizzesForUser(userId: String): Flow<List<QuizEntity>>

    @Query("SELECT * FROM study_quizzes WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getQuizById(id: String, userId: String): QuizEntity?

    @Query("SELECT * FROM study_quiz_questions WHERE quizId = :quizId AND userId = :userId")
    suspend fun getQuestionsForQuiz(quizId: String, userId: String): List<QuizQuestionEntity>

    @Query("SELECT * FROM study_quiz_attempts WHERE userId = :userId ORDER BY completedAt DESC")
    fun getAttemptsForUser(userId: String): Flow<List<QuizAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttemptEntity)

    @Query("DELETE FROM study_quizzes WHERE id = :id AND userId = :userId")
    suspend fun deleteQuiz(id: String, userId: String)

    @Query("DELETE FROM study_quizzes WHERE userId = :userId")
    suspend fun clearQuizzesForUser(userId: String)
}

@Dao
interface StudyGoalDao {
    @Query("SELECT * FROM study_goals WHERE userId = :userId ORDER BY isCompleted ASC, deadline ASC")
    fun getGoalsForUser(userId: String): Flow<List<StudyGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: StudyGoalEntity)

    @Update
    suspend fun updateGoal(goal: StudyGoalEntity)

    @Query("DELETE FROM study_goals WHERE id = :id AND userId = :userId")
    suspend fun deleteGoal(id: String, userId: String)

    @Query("DELETE FROM study_goals WHERE userId = :userId")
    suspend fun clearGoalsForUser(userId: String)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM study_documents WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDocumentsForUser(userId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM study_documents WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getDocumentById(id: String, userId: String): DocumentEntity?

    @Query("SELECT * FROM study_document_chunks WHERE documentId = :documentId AND userId = :userId ORDER BY pageNumber ASC, chunkIndex ASC")
    suspend fun getChunksForDocument(documentId: String, userId: String): List<DocumentChunkEntity>

    @Query("SELECT * FROM study_document_chunks WHERE userId = :userId AND content LIKE '%' || :query || '%' ORDER BY pageNumber ASC LIMIT 10")
    suspend fun searchChunks(userId: String, query: String): List<DocumentChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>)

    @Query("DELETE FROM study_documents WHERE id = :id AND userId = :userId")
    suspend fun deleteDocument(id: String, userId: String)

    @Query("DELETE FROM study_document_chunks WHERE documentId = :documentId AND userId = :userId")
    suspend fun deleteChunksForDocument(documentId: String, userId: String)

    @Query("DELETE FROM study_documents WHERE userId = :userId")
    suspend fun clearDocumentsForUser(userId: String)
}

@Dao
interface CodingDao {
    @Query("SELECT * FROM study_coding_sessions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSessionsForUser(userId: String): Flow<List<CodingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CodingSessionEntity)

    @Query("DELETE FROM study_coding_sessions WHERE id = :id AND userId = :userId")
    suspend fun deleteSession(id: String, userId: String)

    @Query("DELETE FROM study_coding_sessions WHERE userId = :userId")
    suspend fun clearSessionsForUser(userId: String)
}
