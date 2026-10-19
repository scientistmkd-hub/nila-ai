package com.example.core.capability

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.core.model.NetworkStatus
import com.example.core.network.NetworkMonitor
import com.example.data.preferences.NilaUserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class CapabilityStatus(val label: String) {
    AVAILABLE("Available"),
    REQUIRES_PERMISSION("Requires Permission"),
    REQUIRES_NETWORK("Requires Internet"),
    REQUIRES_CONFIG("Configuration Required"),
    UNSUPPORTED("Device Unsupported"),
    DISABLED_BY_USER("Disabled in Settings")
}

data class FeatureCapabilities(
    val camera: CapabilityStatus,
    val microphone: CapabilityStatus,
    val speechRecognition: CapabilityStatus,
    val faceDetection: CapabilityStatus,
    val objectDetection: CapabilityStatus,
    val ocr: CapabilityStatus,
    val cloudAi: CapabilityStatus,
    val imageGeneration: CapabilityStatus,
    val realThreeDGeneration: CapabilityStatus,
    val phoneControl: CapabilityStatus,
    val reminders: CapabilityStatus,
    val offlineMode: CapabilityStatus
)

class FeatureCapabilityManager(
    private val context: Context,
    private val networkMonitor: NetworkMonitor = NetworkMonitor(context)
) {

    fun evaluateCapabilities(settings: NilaUserSettings, networkStatus: NetworkStatus): FeatureCapabilities {
        val hasCameraHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val cameraStatus = when {
            !hasCameraHardware -> CapabilityStatus.UNSUPPORTED
            !hasCameraPermission -> CapabilityStatus.REQUIRES_PERMISSION
            else -> CapabilityStatus.AVAILABLE
        }

        val hasMicHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val micStatus = when {
            !hasMicHardware -> CapabilityStatus.UNSUPPORTED
            !hasMicPermission -> CapabilityStatus.REQUIRES_PERMISSION
            else -> CapabilityStatus.AVAILABLE
        }

        val hasSpeechRecognition = SpeechRecognizer.isRecognitionAvailable(context)
        val speechStatus = when {
            !hasSpeechRecognition -> CapabilityStatus.UNSUPPORTED
            !hasMicPermission -> CapabilityStatus.REQUIRES_PERMISSION
            else -> CapabilityStatus.AVAILABLE
        }

        val isOnline = networkStatus == NetworkStatus.ONLINE && !settings.isOfflineModeEnforced

        val hasBackendConfig = settings.backendServerUrl.isNotBlank()

        val cloudAiStatus = when {
            settings.isOfflineModeEnforced -> CapabilityStatus.DISABLED_BY_USER
            !isOnline -> CapabilityStatus.REQUIRES_NETWORK
            !hasBackendConfig -> CapabilityStatus.REQUIRES_CONFIG
            else -> CapabilityStatus.AVAILABLE
        }

        val imageGenStatus = when {
            settings.isOfflineModeEnforced -> CapabilityStatus.DISABLED_BY_USER
            !isOnline -> CapabilityStatus.REQUIRES_NETWORK
            else -> CapabilityStatus.AVAILABLE // Live online Pollinations / configured endpoint
        }

        // Real 3D generation is unavailable unless a dedicated 3D mesh provider is active
        val threeDStatus = CapabilityStatus.UNSUPPORTED

        val phoneControlStatus = when {
            !settings.isAppControlEnabled -> CapabilityStatus.DISABLED_BY_USER
            else -> CapabilityStatus.AVAILABLE
        }

        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val remindersStatus = when {
            !hasNotificationPermission -> CapabilityStatus.REQUIRES_PERMISSION
            else -> CapabilityStatus.AVAILABLE
        }

        return FeatureCapabilities(
            camera = cameraStatus,
            microphone = micStatus,
            speechRecognition = speechStatus,
            faceDetection = if (hasCameraPermission) CapabilityStatus.AVAILABLE else CapabilityStatus.REQUIRES_PERMISSION,
            objectDetection = if (hasCameraPermission) CapabilityStatus.AVAILABLE else CapabilityStatus.REQUIRES_PERMISSION,
            ocr = if (hasCameraPermission) CapabilityStatus.AVAILABLE else CapabilityStatus.REQUIRES_PERMISSION,
            cloudAi = cloudAiStatus,
            imageGeneration = imageGenStatus,
            realThreeDGeneration = threeDStatus,
            phoneControl = phoneControlStatus,
            reminders = remindersStatus,
            offlineMode = CapabilityStatus.AVAILABLE
        )
    }

    val capabilitiesFlow: Flow<FeatureCapabilities> = networkMonitor.networkStatus.map { netStatus ->
        evaluateCapabilities(NilaUserSettings(), netStatus)
    }
}
