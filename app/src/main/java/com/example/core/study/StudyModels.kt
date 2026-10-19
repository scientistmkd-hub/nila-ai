package com.example.core.study

enum class ExplanationLevel(val label: String, val promptModifier: String) {
    SIMPLE("Simple & Intuitive", "Explain using very simple everyday analogies suitable for quick intuition."),
    BEGINNER("Beginner Friendly", "Explain step-by-step with fundamental definitions and clear examples."),
    INTERMEDIATE("Intermediate Academic", "Provide a comprehensive academic explanation with standard terminology."),
    ADVANCED("Advanced Technical", "Include mathematical rigor, architecture considerations, and deep nuances."),
    EXPERT("Expert Level", "Provide theoretical proofs, edge cases, trade-offs, and optimization strategies."),
    EXAM_READY("Exam Ready", "Structure with definitions, formulas, labeled steps, and concluding key points for exam scoring."),
    INTERVIEW_READY("Interview Ready", "Focus on conceptual clarity, time/space complexity, trade-offs, and practical design.")
}

enum class SocraticHintLevel(val label: String) {
    BEGINNER("Gentle Guidance"),
    INTERMEDIATE("Conceptual Clues"),
    ADVANCED("Minimalist Pointers")
}

enum class StudyModeTab(val displayName: String) {
    OVERVIEW("Dashboard"),
    SUBJECTS("Subjects"),
    CAMERA_SOLVER("Scan & Solve"),
    QUIZ("MCQ Quiz"),
    FLASHCARDS("Flashcards"),
    CODING("Coding Tutor"),
    DOCUMENTS("Documents"),
    VIVA("Viva Prep"),
    ROADMAP("Roadmap")
}

data class StepByStepSolution(
    val question: String,
    val knownInformation: String,
    val requiredResult: String,
    val coreConcept: String,
    val steps: List<String>,
    val verification: String,
    val finalAnswer: String,
    val summary: String,
    val explanationLevel: ExplanationLevel = ExplanationLevel.INTERMEDIATE
)

data class SocraticState(
    val problem: String,
    val currentStep: Int = 1,
    val guidingQuestion: String,
    val hint: String,
    val hintLevel: SocraticHintLevel = SocraticHintLevel.INTERMEDIATE,
    val conversationHistory: List<Pair<String, String>> = emptyList(), // Pair(speaker, text)
    val isSolved: Boolean = false
)

enum class SpacedRepetitionRating {
    KNOW,
    ALMOST,
    DONT_KNOW
}

data class DocumentCitation(
    val documentTitle: String,
    val pageNumber: Int,
    val chunkIndex: Int,
    val excerpt: String
)

data class DocumentQAResponse(
    val answer: String,
    val citations: List<DocumentCitation>,
    val confidence: Float
)
