package com.example.core.model

/**
 * Commands that Nila's Phone Control provider can execute.
 * Conforms to Phase 6 Requirement 5.
 */
enum class PhoneCommandType(val label: String, val defaultRisk: RiskLevel) {
    OPEN_APP("Open Application", RiskLevel.LOW),
    OPEN_URL("Open Web Link", RiskLevel.LOW),
    GO_BACK("Navigate Back", RiskLevel.LOW),
    GO_HOME("Return Home", RiskLevel.LOW),
    OPEN_RECENTS("Open App Switcher", RiskLevel.LOW),
    TAKE_PHOTO("Capture Photo", RiskLevel.LOW),
    OPEN_CAMERA("Open Camera", RiskLevel.LOW),
    PLAY_MEDIA("Play Audio/Video", RiskLevel.LOW),
    PAUSE_MEDIA("Pause Media", RiskLevel.LOW),
    NEXT_MEDIA("Next Track", RiskLevel.LOW),
    PREVIOUS_MEDIA("Previous Track", RiskLevel.LOW),
    VOLUME_UP("Volume Up", RiskLevel.LOW),
    VOLUME_DOWN("Volume Down", RiskLevel.LOW),
    MUTE("Mute Audio", RiskLevel.LOW),
    SET_BRIGHTNESS("Adjust Brightness", RiskLevel.LOW),
    OPEN_SETTINGS("Open Device Settings", RiskLevel.LOW),
    OPEN_WIFI_SETTINGS("Open Wi-Fi Settings", RiskLevel.LOW),
    OPEN_BLUETOOTH_SETTINGS("Open Bluetooth Settings", RiskLevel.LOW),
    OPEN_NOTIFICATION_SETTINGS("Open Notification Settings", RiskLevel.LOW),
    SCROLL("Controlled Scroll", RiskLevel.LOW),
    TAP("Simulate Tap", RiskLevel.MEDIUM),
    SWIPE("Simulate Swipe", RiskLevel.MEDIUM),
    TYPE_TEXT("Type Text", RiskLevel.MEDIUM),
    SEND_MESSAGE("Send Message", RiskLevel.HIGH),
    PREPARE_MESSAGE("Prepare Message", RiskLevel.MEDIUM)
}

/**
 * Execution outcome for phone control operations.
 */
enum class PhoneCommandStatus {
    SUCCESS,
    FAILED,
    NOT_SUPPORTED,
    PERMISSION_REQUIRED,
    CANCELLED,
    BLOCKED,
    TIMEOUT
}

data class PhoneCommandResult(
    val commandType: PhoneCommandType,
    val status: PhoneCommandStatus,
    val message: String,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * User preference for action confirmations.
 */
enum class ConfirmationPreference {
    ALWAYS_ASK,
    ASK_RISKY_ONLY,
    NEVER_ASK_LOW
}

/**
 * Precision timer states.
 */
enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}

/**
 * Pomodoro focus cycle phases.
 */
enum class PomodoroPhase(val label: String) {
    WORK_FOCUS("Deep Work Focus"),
    SHORT_BREAK("Short Rest Break"),
    LONG_BREAK("Extended Restoration")
}

/**
 * Productivity Task statuses.
 */
enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    ARCHIVED("Archived")
}

enum class TaskPriority(val label: String) {
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High"),
    URGENT("Urgent")
}

enum class ReminderStatus(val label: String) {
    ACTIVE("Active"),
    SNOOZED("Snoozed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

enum class AutomationStatus(val label: String) {
    DISABLED("Disabled"),
    READY("Ready"),
    RUNNING("Running"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

enum class RoutineType(val label: String) {
    MORNING("Morning Start"),
    STUDY("Study Session"),
    NIGHT("Night Wrap-up"),
    WORK("Work Routine"),
    CUSTOM("Custom Routine")
}

data class RoutineStep(
    val id: String,
    val title: String,
    val actionType: String,
    val parameter: String = "",
    val requiresConfirmation: Boolean = false,
    val isCompleted: Boolean = false
)
