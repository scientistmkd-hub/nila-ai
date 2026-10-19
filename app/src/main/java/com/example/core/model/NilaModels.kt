package com.example.core.model

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    CAMERA_ACTIVE,
    GESTURE_DETECTED,
    EYE_CONTACT,
    GENERATING,
    STUDYING,
    DOCUMENT_ANALYZING,
    CODING,
    CONTROL_ACTIVE,
    TIMER_ACTIVE,
    AUTOMATION_ACTIVE,
    SUCCESS,
    ERROR,
    OFFLINE
}

enum class OrbStyle(val displayName: String) {
    ENERGY("Quantum Energy"),
    GALAXY("Cosmic Galaxy"),
    LIQUID("Fluid Cyan"),
    CRYSTAL("Prism Crystal"),
    NEON("Cyber Neon"),
    MINIMAL("Sleek Minimal")
}

enum class ThemeMode {
    DARK,
    LIGHT,
    AMOLED
}

enum class NetworkStatus {
    ONLINE,
    OFFLINE,
    UNSTABLE
}

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}

enum class MemoryCategory(val label: String) {
    USER_PREFERENCE("User Preference"),
    PERSONAL_INFO("Personal Info"),
    PERSONAL_DETAIL("Personal Detail"),
    GOAL("Goal & Aspiration"),
    TASK("Task & Reminder"),
    STUDY_INFO("Study & Knowledge"),
    PERSON_PROFILE("Person Profile"),
    OBJECT_PROFILE("Object Profile"),
    OBJECT("Object Memory"),
    CONVERSATION_MEMORY("Conversation Memory"),
    CUSTOM("Custom Memory")
}

data class PersonMatchResult(
    val matchedPersonName: String,
    val relationship: String,
    val personId: String,
    val confidence: Float,
    val isConfidentMatch: Boolean,
    val notes: String = ""
)

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    BLOCKED
}

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR,
    UNAVAILABLE
}

enum class FaceState {
    NO_FACE,
    FACE_DETECTED,
    MULTIPLE_FACES,
    LOW_CONFIDENCE,
    TRACKING
}

enum class EyeInteractionState {
    NO_FACE,
    FACE_DETECTED,
    LOOKING_TOWARD_DEVICE,
    LOOKING_AWAY,
    LOW_CONFIDENCE
}

enum class HandGesture(val displayName: String, val defaultAction: String) {
    THUMBS_UP("Thumbs Up", "Confirm"),
    THUMBS_DOWN("Thumbs Down", "Cancel"),
    OPEN_PALM("Open Palm", "Pause / Stop"),
    INDEX_POINT("Index Point", "Select"),
    V_SIGN("V-Sign / Peace", "Next"),
    PINCH("Pinch", "Grab / Focus"),
    FIST("Fist", "Hold"),
    WAVE("Wave", "Acknowledge"),
    UNKNOWN("None", "None")
}

data class GestureResult(
    val gesture: HandGesture,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

enum class VisionMode(val label: String) {
    ORIGINAL("Original"),
    FACE_DETECTION("Face Detection"),
    GESTURES("Gesture Detection"),
    OCR_TEXT("OCR Text"),
    OBJECT_DETECTION("Object Detection"),
    STUDY_SCAN("Study Scan"),
    DOCUMENT_SCAN("Document Scan"),
    VISION_ASSISTANT("Vision Assistant");

    companion object {
        val FACE_ATTENTION = FACE_DETECTION
        val OBJECT_RECOGNITION = OBJECT_DETECTION
        val GESTURE_DETECTION = GESTURES
    }
}

enum class VisionQuality(val targetFps: Int, val label: String) {
    LOW(3, "Low (Battery Saver)"),
    MEDIUM(7, "Medium (Balanced)"),
    HIGH(15, "High (Full Precision)")
}

data class DetectedFaceInfo(
    val boundingBox: android.graphics.Rect,
    val trackingId: Int?,
    val headEulerY: Float,
    val headEulerZ: Float,
    val leftEyeOpenProb: Float?,
    val rightEyeOpenProb: Float?,
    val isLookingAtDevice: Boolean
)

data class NilaState(
    val orbState: OrbState = OrbState.IDLE,
    val orbStyle: OrbStyle = OrbStyle.ENERGY,
    val networkStatus: NetworkStatus = NetworkStatus.ONLINE,
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val isCameraActive: Boolean = false,
    val isGenerating: Boolean = false,
    val isOffline: Boolean = false,
    val activeUser: String = "guest_default",
    val statusMessage: String = "Ready",
    val errorMessage: String? = null
)

sealed class AppError(open val userFriendlyMessage: String) : Exception(userFriendlyMessage) {
    data class NetworkError(override val userFriendlyMessage: String = "Network connection is unavailable or unstable.") : AppError(userFriendlyMessage)
    data class ProviderError(override val userFriendlyMessage: String = "The requested AI provider is not configured or reached quota.") : AppError(userFriendlyMessage)
    data class PermissionError(override val userFriendlyMessage: String = "Permission required to use this feature.") : AppError(userFriendlyMessage)
    data class CameraError(override val userFriendlyMessage: String = "Camera hardware is currently unavailable.") : AppError(userFriendlyMessage)
    data class MicrophoneError(override val userFriendlyMessage: String = "Microphone input is currently unavailable.") : AppError(userFriendlyMessage)
    data class DatabaseError(override val userFriendlyMessage: String = "Local storage error occurred.") : AppError(userFriendlyMessage)
    data class GenerationError(override val userFriendlyMessage: String = "Asset generation could not be completed.") : AppError(userFriendlyMessage)
    data class OfflineError(override val userFriendlyMessage: String = "This feature requires an active internet connection.") : AppError(userFriendlyMessage)
    data class UnknownError(override val userFriendlyMessage: String = "Something went wrong while contacting Nila's engine.") : AppError(userFriendlyMessage)
}
