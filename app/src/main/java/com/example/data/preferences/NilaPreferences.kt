package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.OrbStyle
import com.example.core.model.ThemeMode
import com.example.core.model.VisionQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nila_settings")

data class NilaUserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val orbStyle: OrbStyle = OrbStyle.ENERGY,
    val graphicsQuality: String = "HIGH",
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val voiceLanguage: String = "English",
    val isTtsEnabled: Boolean = true,
    val isControlModeEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val isReducedMotion: Boolean = false,
    val isOfflineModeEnforced: Boolean = false,
    // Phase 2 Settings
    val isGestureControlEnabled: Boolean = true,
    val isEyeAttentionEnabled: Boolean = true,
    val isWakeWordEnabled: Boolean = true,
    val gestureSensitivity: Float = 0.82f,
    val visionQuality: VisionQuality = VisionQuality.MEDIUM,
    val vSignAction: String = "Next", // "Next" or "Take Photo"
    // Phase 3 Settings
    val isPersonRecognitionEnabled: Boolean = true,
    val isObjectRecognitionEnabled: Boolean = true,
    val personMatchThreshold: Float = 0.84f,
    val isCloudSyncEnabled: Boolean = false,
    // Phase 6 Control & Productivity Settings
    val isVoiceControlEnabled: Boolean = true,
    val isAppControlEnabled: Boolean = true,
    val isAutomationEnabled: Boolean = false,
    val isBackgroundAssistanceEnabled: Boolean = false,
    val confirmationPreference: String = "ASK_RISKY_ONLY", // ALWAYS_ASK, ASK_RISKY_ONLY, NEVER_ASK_LOW
    val allowedApps: Set<String> = setOf("WhatsApp", "YouTube", "Chrome", "Instagram", "Camera", "Gallery", "Spotify", "Calculator", "Maps", "Settings"),
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val pomodoroLongBreakMinutes: Int = 15,
    val pomodoroCycles: Int = 4,
    // Production Backend Architecture
    val backendServerUrl: String = "https://nila-ai-fo5z.onrender.com",
    val backendAuthToken: String = ""
)

class NilaPreferences(private val context: Context) {
    companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_ORB_STYLE = stringPreferencesKey("orb_style")
        val KEY_GRAPHICS = stringPreferencesKey("graphics_quality")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_VOICE_LANG = stringPreferencesKey("voice_lang")
        val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val KEY_CONTROL_MODE = booleanPreferencesKey("control_mode_enabled")
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val KEY_OFFLINE_ENFORCED = booleanPreferencesKey("offline_mode_enforced")
        val KEY_GESTURE_ENABLED = booleanPreferencesKey("gesture_control_enabled")
        val KEY_EYE_ATTENTION = booleanPreferencesKey("eye_attention_enabled")
        val KEY_WAKE_WORD = booleanPreferencesKey("wake_word_enabled")
        val KEY_GESTURE_SENSITIVITY = floatPreferencesKey("gesture_sensitivity")
        val KEY_VISION_QUALITY = stringPreferencesKey("vision_quality")
        val KEY_VSIGN_ACTION = stringPreferencesKey("vsign_action")
        val KEY_PERSON_RECOGNITION = booleanPreferencesKey("person_recognition_enabled")
        val KEY_OBJECT_RECOGNITION = booleanPreferencesKey("object_recognition_enabled")
        val KEY_MATCH_THRESHOLD = floatPreferencesKey("person_match_threshold")
        val KEY_CLOUD_SYNC = booleanPreferencesKey("cloud_sync_enabled")
        val KEY_VOICE_CONTROL = booleanPreferencesKey("voice_control_enabled")
        val KEY_APP_CONTROL = booleanPreferencesKey("app_control_enabled")
        val KEY_AUTOMATION = booleanPreferencesKey("automation_enabled")
        val KEY_BG_ASSISTANCE = booleanPreferencesKey("bg_assistance_enabled")
        val KEY_CONFIRM_PREF = stringPreferencesKey("confirmation_preference")
        val KEY_ALLOWED_APPS = stringSetPreferencesKey("allowed_apps_set")
        val KEY_POMO_FOCUS = intPreferencesKey("pomodoro_focus_min")
        val KEY_POMO_BREAK = intPreferencesKey("pomodoro_break_min")
        val KEY_POMO_LONG_BREAK = intPreferencesKey("pomodoro_long_break_min")
        val KEY_POMO_CYCLES = intPreferencesKey("pomodoro_cycles_count")
        val KEY_BACKEND_URL = stringPreferencesKey("backend_server_url")
        val KEY_BACKEND_TOKEN = stringPreferencesKey("backend_auth_token")
    }

    val settingsFlow: Flow<NilaUserSettings> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[KEY_THEME] ?: ThemeMode.DARK.name
        val themeMode = try { ThemeMode.valueOf(themeStr) } catch (_: Exception) { ThemeMode.DARK }

        val orbStr = prefs[KEY_ORB_STYLE] ?: OrbStyle.ENERGY.name
        val orbStyle = try { OrbStyle.valueOf(orbStr) } catch (_: Exception) { OrbStyle.ENERGY }

        val visionQualityStr = prefs[KEY_VISION_QUALITY] ?: VisionQuality.MEDIUM.name
        val visionQuality = try { VisionQuality.valueOf(visionQualityStr) } catch (_: Exception) { VisionQuality.MEDIUM }

        val defaultApps = setOf("WhatsApp", "YouTube", "Chrome", "Instagram", "Camera", "Gallery", "Spotify", "Calculator", "Maps", "Settings")

        NilaUserSettings(
            themeMode = themeMode,
            orbStyle = orbStyle,
            graphicsQuality = prefs[KEY_GRAPHICS] ?: "HIGH",
            speechRate = prefs[KEY_SPEECH_RATE] ?: 1.0f,
            speechPitch = prefs[KEY_SPEECH_PITCH] ?: 1.0f,
            voiceLanguage = prefs[KEY_VOICE_LANG] ?: "English",
            isTtsEnabled = prefs[KEY_TTS_ENABLED] ?: true,
            isControlModeEnabled = prefs[KEY_CONTROL_MODE] ?: false,
            isOnboardingCompleted = prefs[KEY_ONBOARDING] ?: false,
            isReducedMotion = prefs[KEY_REDUCED_MOTION] ?: false,
            isOfflineModeEnforced = prefs[KEY_OFFLINE_ENFORCED] ?: false,
            isGestureControlEnabled = prefs[KEY_GESTURE_ENABLED] ?: true,
            isEyeAttentionEnabled = prefs[KEY_EYE_ATTENTION] ?: true,
            isWakeWordEnabled = prefs[KEY_WAKE_WORD] ?: true,
            gestureSensitivity = prefs[KEY_GESTURE_SENSITIVITY] ?: 0.82f,
            visionQuality = visionQuality,
            vSignAction = prefs[KEY_VSIGN_ACTION] ?: "Next",
            isPersonRecognitionEnabled = prefs[KEY_PERSON_RECOGNITION] ?: true,
            isObjectRecognitionEnabled = prefs[KEY_OBJECT_RECOGNITION] ?: true,
            personMatchThreshold = prefs[KEY_MATCH_THRESHOLD] ?: 0.84f,
            isCloudSyncEnabled = prefs[KEY_CLOUD_SYNC] ?: false,
            isVoiceControlEnabled = prefs[KEY_VOICE_CONTROL] ?: true,
            isAppControlEnabled = prefs[KEY_APP_CONTROL] ?: true,
            isAutomationEnabled = prefs[KEY_AUTOMATION] ?: false,
            isBackgroundAssistanceEnabled = prefs[KEY_BG_ASSISTANCE] ?: false,
            confirmationPreference = prefs[KEY_CONFIRM_PREF] ?: "ASK_RISKY_ONLY",
            allowedApps = prefs[KEY_ALLOWED_APPS] ?: defaultApps,
            pomodoroFocusMinutes = prefs[KEY_POMO_FOCUS] ?: 25,
            pomodoroBreakMinutes = prefs[KEY_POMO_BREAK] ?: 5,
            pomodoroLongBreakMinutes = prefs[KEY_POMO_LONG_BREAK] ?: 15,
            pomodoroCycles = prefs[KEY_POMO_CYCLES] ?: 4,
            backendServerUrl = prefs[KEY_BACKEND_URL] ?: "https://nila-ai-fo5z.onrender.com",
            backendAuthToken = prefs[KEY_BACKEND_TOKEN] ?: ""
        )
    }

    suspend fun setBackendServerUrl(url: String) {
        context.dataStore.edit { it[KEY_BACKEND_URL] = url.trim() }
    }

    suspend fun setBackendAuthToken(token: String) {
        context.dataStore.edit { it[KEY_BACKEND_TOKEN] = token.trim() }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setOrbStyle(style: OrbStyle) {
        context.dataStore.edit { it[KEY_ORB_STYLE] = style.name }
    }

    suspend fun setVoiceLanguage(language: String) {
        context.dataStore.edit { it[KEY_VOICE_LANG] = language }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = rate }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[KEY_SPEECH_PITCH] = pitch }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TTS_ENABLED] = enabled }
    }

    suspend fun setControlModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CONTROL_MODE] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING] = completed }
    }

    suspend fun setReducedMotion(reduced: Boolean) {
        context.dataStore.edit { it[KEY_REDUCED_MOTION] = reduced }
    }

    suspend fun setOfflineModeEnforced(enforced: Boolean) {
        context.dataStore.edit { it[KEY_OFFLINE_ENFORCED] = enforced }
    }

    suspend fun setGestureControlEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GESTURE_ENABLED] = enabled }
    }

    suspend fun setEyeAttentionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_EYE_ATTENTION] = enabled }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_WAKE_WORD] = enabled }
    }

    suspend fun setGestureSensitivity(sensitivity: Float) {
        context.dataStore.edit { it[KEY_GESTURE_SENSITIVITY] = sensitivity }
    }

    suspend fun setVisionQuality(quality: VisionQuality) {
        context.dataStore.edit { it[KEY_VISION_QUALITY] = quality.name }
    }

    suspend fun setVSignAction(action: String) {
        context.dataStore.edit { it[KEY_VSIGN_ACTION] = action }
    }

    suspend fun setPersonRecognitionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PERSON_RECOGNITION] = enabled }
    }

    suspend fun setObjectRecognitionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_OBJECT_RECOGNITION] = enabled }
    }

    suspend fun setPersonMatchThreshold(threshold: Float) {
        context.dataStore.edit { it[KEY_MATCH_THRESHOLD] = threshold }
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CLOUD_SYNC] = enabled }
    }

    suspend fun setVoiceControlEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VOICE_CONTROL] = enabled }
    }

    suspend fun setAppControlEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_APP_CONTROL] = enabled }
    }

    suspend fun setAutomationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTOMATION] = enabled }
    }

    suspend fun setBackgroundAssistanceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BG_ASSISTANCE] = enabled }
    }

    suspend fun setConfirmationPreference(preference: String) {
        context.dataStore.edit { it[KEY_CONFIRM_PREF] = preference }
    }

    suspend fun setAllowedApps(apps: Set<String>) {
        context.dataStore.edit { it[KEY_ALLOWED_APPS] = apps }
    }

    suspend fun toggleAllowedApp(appName: String, isAllowed: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_ALLOWED_APPS] ?: setOf("WhatsApp", "YouTube", "Chrome", "Instagram", "Camera", "Gallery", "Spotify", "Calculator", "Maps", "Settings")).toMutableSet()
            if (isAllowed) current.add(appName) else current.remove(appName)
            prefs[KEY_ALLOWED_APPS] = current
        }
    }

    suspend fun setPomodoroDurations(focusMin: Int, breakMin: Int, longBreakMin: Int, cycles: Int = 4) {
        context.dataStore.edit {
            it[KEY_POMO_FOCUS] = focusMin
            it[KEY_POMO_BREAK] = breakMin
            it[KEY_POMO_LONG_BREAK] = longBreakMin
            it[KEY_POMO_CYCLES] = cycles
        }
    }
}
