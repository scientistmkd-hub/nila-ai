package com.example.core.control

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import com.example.core.model.*
import com.example.data.local.NilaDatabase
import com.example.data.local.entity.ControlLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.UUID

/**
 * Clean phone control provider using Android Intents first.
 * Conforms to Phase 6 Requirement 5, 6, 7, 8, 10, 11.
 */
class PhoneControlProvider(
    private val context: Context,
    private val db: NilaDatabase = NilaDatabase.getInstance(context)
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /**
     * Executes phone commands with allowlist checking, risk validation, and audit logging.
     */
    suspend fun executeCommand(
        commandType: PhoneCommandType,
        targetApp: String? = null,
        parameter: String? = null,
        allowedApps: Set<String>,
        userId: String,
        source: String = "BUTTON"
    ): PhoneCommandResult = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()

        // 1. Strict Security Policy (BLOCKED risk level check)
        val paramLower = parameter?.lowercase() ?: ""
        if (paramLower.contains("password") || paramLower.contains("otp") || paramLower.contains("banking") ||
            paramLower.contains("transfer money") || paramLower.contains("cvv") || paramLower.contains("pin")
        ) {
            val blockedResult = PhoneCommandResult(
                commandType = commandType,
                status = PhoneCommandStatus.BLOCKED,
                message = "Action Blocked: Nila strictly prohibits automated entry of passwords, OTPs, or financial transactions.",
                riskLevel = RiskLevel.BLOCKED
            )
            logActivity(userId, commandType.label, source, "BLOCKED", "BLOCKED", blockedResult.message)
            return@withContext blockedResult
        }

        // 2. Allowed Apps Policy check for App-related actions
        if (targetApp != null) {
            val isAllowed = allowedApps.any { it.equals(targetApp, ignoreCase = true) }
            if (!isAllowed) {
                val notAllowedResult = PhoneCommandResult(
                    commandType = commandType,
                    status = PhoneCommandStatus.BLOCKED,
                    message = "This app is not enabled for Nila Control. You can enable '$targetApp' in Control Settings.",
                    riskLevel = RiskLevel.LOW
                )
                logActivity(userId, "Access $targetApp", source, "LOW", "BLOCKED", notAllowedResult.message)
                return@withContext notAllowedResult
            }
        }

        // 3. Execution via Standard Android Intents
        val result = try {
            when (commandType) {
                PhoneCommandType.OPEN_APP -> openApplication(targetApp ?: parameter ?: "")
                PhoneCommandType.OPEN_URL -> openUrl(parameter ?: "https://www.google.com")
                PhoneCommandType.OPEN_CAMERA -> openCamera()
                PhoneCommandType.TAKE_PHOTO -> openCamera()
                PhoneCommandType.OPEN_SETTINGS -> openSettings(Settings.ACTION_SETTINGS)
                PhoneCommandType.OPEN_WIFI_SETTINGS -> openSettings(Settings.ACTION_WIFI_SETTINGS)
                PhoneCommandType.OPEN_BLUETOOTH_SETTINGS -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
                PhoneCommandType.OPEN_NOTIFICATION_SETTINGS -> openNotificationSettings()
                PhoneCommandType.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
                PhoneCommandType.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
                PhoneCommandType.MUTE -> muteAudio()
                PhoneCommandType.PLAY_MEDIA, PhoneCommandType.PAUSE_MEDIA -> toggleMediaPlayPause()
                PhoneCommandType.NEXT_MEDIA -> skipMediaTrack(KeyEvent.KEYCODE_MEDIA_NEXT)
                PhoneCommandType.PREVIOUS_MEDIA -> skipMediaTrack(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                PhoneCommandType.GO_HOME -> returnHome()
                PhoneCommandType.PREPARE_MESSAGE -> prepareWhatsAppMessage(parameter ?: "", targetApp)
                PhoneCommandType.SEND_MESSAGE -> sendPreparedMessage(parameter ?: "", targetApp)
                else -> PhoneCommandResult(
                    commandType = commandType,
                    status = PhoneCommandStatus.NOT_SUPPORTED,
                    message = "Command ${commandType.label} requires special accessibility integration."
                )
            }
        } catch (e: Exception) {
            PhoneCommandResult(
                commandType = commandType,
                status = PhoneCommandStatus.FAILED,
                message = "Execution failed: ${e.localizedMessage}"
            )
        }

        logActivity(
            userId = userId,
            action = "${commandType.label}: ${targetApp ?: parameter ?: ""}",
            source = source,
            riskLevel = commandType.defaultRisk.name,
            result = result.status.name,
            details = result.message
        )

        result
    }

    private fun openApplication(appName: String): PhoneCommandResult {
        val lower = appName.lowercase()
        val intent = when {
            lower.contains("whatsapp") -> {
                context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com"))
            }
            lower.contains("youtube") -> {
                context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            }
            lower.contains("chrome") || lower.contains("browser") -> {
                context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            }
            lower.contains("spotify") || lower.contains("music") -> {
                context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
            }
            lower.contains("map") -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=directions"))
            }
            lower.contains("camera") -> {
                Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            }
            lower.contains("settings") -> {
                Intent(Settings.ACTION_SETTINGS)
            }
            lower.contains("calculator") -> {
                context.packageManager.getLaunchIntentForPackage("com.google.android.calculator")
                    ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
            }
            else -> {
                context.packageManager.getLaunchIntentForPackage(appName)
            }
        }

        return if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            PhoneCommandResult(
                commandType = PhoneCommandType.OPEN_APP,
                status = PhoneCommandStatus.SUCCESS,
                message = "Opened $appName successfully."
            )
        } else {
            PhoneCommandResult(
                commandType = PhoneCommandType.OPEN_APP,
                status = PhoneCommandStatus.FAILED,
                message = "App '$appName' is not installed or available on this device."
            )
        }
    }

    private fun openUrl(url: String): PhoneCommandResult {
        val parsedUri = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Uri.parse("https://$url")
        } else {
            Uri.parse(url)
        }
        val intent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return PhoneCommandResult(
            commandType = PhoneCommandType.OPEN_URL,
            status = PhoneCommandStatus.SUCCESS,
            message = "Opened link in browser."
        )
    }

    private fun openCamera(): PhoneCommandResult {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return PhoneCommandResult(
            commandType = PhoneCommandType.OPEN_CAMERA,
            status = PhoneCommandStatus.SUCCESS,
            message = "Camera opened."
        )
    }

    private fun openSettings(action: String): PhoneCommandResult {
        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return PhoneCommandResult(
            commandType = PhoneCommandType.OPEN_SETTINGS,
            status = PhoneCommandStatus.SUCCESS,
            message = "Opened device configuration."
        )
    }

    private fun openNotificationSettings(): PhoneCommandResult {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return PhoneCommandResult(
            commandType = PhoneCommandType.OPEN_NOTIFICATION_SETTINGS,
            status = PhoneCommandStatus.SUCCESS,
            message = "Opened notification preferences."
        )
    }

    private fun adjustVolume(direction: Int): PhoneCommandResult {
        audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return PhoneCommandResult(
            commandType = if (direction == AudioManager.ADJUST_RAISE) PhoneCommandType.VOLUME_UP else PhoneCommandType.VOLUME_DOWN,
            status = PhoneCommandStatus.SUCCESS,
            message = "Volume adjusted."
        )
    }

    private fun muteAudio(): PhoneCommandResult {
        audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        return PhoneCommandResult(
            commandType = PhoneCommandType.MUTE,
            status = PhoneCommandStatus.SUCCESS,
            message = "Audio muted."
        )
    }

    private fun toggleMediaPlayPause(): PhoneCommandResult {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        audioManager?.dispatchMediaKeyEvent(downEvent)
        audioManager?.dispatchMediaKeyEvent(upEvent)
        return PhoneCommandResult(
            commandType = PhoneCommandType.PLAY_MEDIA,
            status = PhoneCommandStatus.SUCCESS,
            message = "Media play/pause toggled."
        )
    }

    private fun skipMediaTrack(keyCode: Int): PhoneCommandResult {
        val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val up = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(down)
        audioManager?.dispatchMediaKeyEvent(up)
        return PhoneCommandResult(
            commandType = if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) PhoneCommandType.NEXT_MEDIA else PhoneCommandType.PREVIOUS_MEDIA,
            status = PhoneCommandStatus.SUCCESS,
            message = "Track skipped."
        )
    }

    private fun returnHome(): PhoneCommandResult {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return PhoneCommandResult(
            commandType = PhoneCommandType.GO_HOME,
            status = PhoneCommandStatus.SUCCESS,
            message = "Returned to Home screen."
        )
    }

    /**
     * Prepares a message intent without sending automatically.
     * Conforms to Requirement 10 (WhatsApp safe actions).
     */
    fun prepareWhatsAppMessage(message: String, recipientPhone: String?): PhoneCommandResult {
        val encodedMessage = try { URLEncoder.encode(message, "UTF-8") } catch (_: Exception) { message }
        val uriStr = if (!recipientPhone.isNullOrBlank()) {
            "https://api.whatsapp.com/send?phone=$recipientPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return PhoneCommandResult(
            commandType = PhoneCommandType.PREPARE_MESSAGE,
            status = PhoneCommandStatus.SUCCESS,
            message = "Prepared message draft in WhatsApp: \"$message\""
        )
    }

    fun sendPreparedMessage(message: String, recipientPhone: String?): PhoneCommandResult {
        return prepareWhatsAppMessage(message, recipientPhone)
    }

    private suspend fun logActivity(
        userId: String,
        action: String,
        source: String,
        riskLevel: String,
        result: String,
        details: String
    ) = withContext(Dispatchers.IO) {
        try {
            val log = ControlLogEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                action = action,
                source = source,
                riskLevel = riskLevel,
                result = result,
                details = details,
                timestamp = System.currentTimeMillis()
            )
            db.controlLogDao().insertControlLog(log)
        } catch (_: Exception) {}
    }
}
