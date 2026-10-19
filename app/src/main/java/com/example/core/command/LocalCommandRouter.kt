package com.example.core.command

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.core.memory.MemoryIntentParser
import com.example.core.memory.MemoryIntentResult
import com.example.core.model.RiskLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class CommandResult {
    data class Handled(
        val reply: String,
        val navigationTarget: String? = null,
        val intent: Intent? = null,
        val shouldTakePhoto: Boolean = false,
        val shouldStopSpeech: Boolean = false
    ) : CommandResult()
    data class NeedsConfirmation(val actionDescription: String, val riskLevel: RiskLevel, val pendingAction: () -> Unit) : CommandResult()
    data class Blocked(val reason: String) : CommandResult()
    object NotACommand : CommandResult()
}

class LocalCommandRouter(private val context: Context) {

    fun evaluate(input: String): CommandResult {
        val clean = normalizeMultilingualInput(input)

        // Wake word & Identity
        if (clean == "nila" || clean == "hey nila" || clean == "hi nila" || clean == "hello nila" || clean == "nila assistant") {
            return CommandResult.Handled("I am here.")
        }
        if (clean.contains("who are you") || clean.contains("what are you") || clean.contains("your name") || clean.contains("yaaru nee")) {
            return CommandResult.Handled("I am Nila, your personal multimodal AI assistant. I interact through voice, camera perception, gestures, and memory with privacy-first architecture.")
        }

        // Time / Date (English + Tanglish)
        if (clean.contains("time is it") || clean == "time" || clean.contains("current time") || clean.contains("time enna") || clean.contains("neram enna")) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            return CommandResult.Handled("The current time is ${sdf.format(Date())}.")
        }
        if (clean.contains("today's date") || clean.contains("what date") || clean == "date" || clean.contains("date enna") || clean.contains("thethi enna")) {
            val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            return CommandResult.Handled("Today is ${sdf.format(Date())}.")
        }

        // Stop / Cancel / Interrupt Speech
        if (clean == "stop" || clean == "cancel" || clean == "pause" || clean == "nila stop" || clean == "niruthu" || clean == "pesadha") {
            return CommandResult.Handled("Stopped.", shouldStopSpeech = true)
        }

        // Photo capture command
        if (clean == "take photo" || clean == "capture" || clean == "click photo" || clean.contains("photo edu") || clean.contains("padam edu")) {
            return CommandResult.Handled("Capturing frame.", navigationTarget = "camera", shouldTakePhoto = true)
        }

        // Phase 3 Natural Language Memory Intents
        val memoryIntent = MemoryIntentParser.parse(input)
        when (memoryIntent) {
            is MemoryIntentResult.ClearAllPrompt -> {
                return CommandResult.NeedsConfirmation(
                    actionDescription = "Permanently wipe all private memories and profiles for your active profile?",
                    riskLevel = RiskLevel.HIGH,
                    pendingAction = { /* handled by confirmation */ }
                )
            }
            is MemoryIntentResult.SaveMemoryPrompt -> {
                val detail = if (memoryIntent.personName != null) {
                    "remember ${memoryIntent.personName} as your ${memoryIntent.relationship}"
                } else {
                    "save '${memoryIntent.title}'"
                }
                return CommandResult.Handled(
                    reply = "I can $detail. Tap save to confirm in your Memory Bank.",
                    navigationTarget = "memory"
                )
            }
            is MemoryIntentResult.DeletePrompt -> {
                return CommandResult.Handled(
                    reply = "Open Memory Bank to select and delete '${memoryIntent.query}'.",
                    navigationTarget = "memory"
                )
            }
            is MemoryIntentResult.LookupQuery -> {
                val label = if (memoryIntent.query.isNotBlank()) "for '${memoryIntent.query}'" else "list"
                return CommandResult.Handled(
                    reply = "Opening your private Memory Bank $label.",
                    navigationTarget = "memory"
                )
            }
            is MemoryIntentResult.None -> {
                // Continue to navigation and apps
            }
        }

        // Phase 4 Voice Creation Commands: "create an image of...", "generate anime...", "create a 3d model..."
        if (clean.contains("create an image") || clean.contains("create image") || clean.contains("generate image") || clean.startsWith("draw ")) {
            val promptSubject = input.substringAfter("image of", "").ifBlank {
                input.substringAfter("generate image", "").ifBlank {
                    input.substringAfter("create", "").ifBlank { input.substringAfter("draw", "") }
                }
            }.trim()
            return CommandResult.Handled(
                reply = "Opening Creation Studio for \"${promptSubject.ifBlank { "your concept" }}\".",
                navigationTarget = "create"
            )
        }
        if (clean.contains("anime character") || clean.contains("generate anime") || clean.contains("create anime")) {
            return CommandResult.Handled(
                reply = "Launching Anime Creation Studio.",
                navigationTarget = "create"
            )
        }
        if (clean.contains("3d model") || clean.contains("create 3d") || clean.contains("create a 3d") || clean.contains("generate 3d")) {
            return CommandResult.Handled(
                reply = "Launching 3D Mesh Creation Studio.",
                navigationTarget = "create"
            )
        }

        // Navigation commands (English + Tanglish: "open pannu", "poi", "kaatu")
        if (clean == "go home" || clean == "home" || clean == "veetukku po") {
            return CommandResult.Handled("Returning to Home.", navigationTarget = "home")
        }
        if (clean == "open settings" || clean == "settings" || clean.contains("settings open") || clean.contains("settings poi")) {
            return CommandResult.Handled("Opening Settings.", navigationTarget = "settings")
        }
        if (clean == "open camera" || clean == "camera" || clean == "vision" || clean.contains("camera open") || clean.contains("camera ku po")) {
            return CommandResult.Handled("Switching to Camera Vision.", navigationTarget = "camera")
        }
        if (clean == "open chat" || clean == "chat" || clean.contains("chat open") || clean.contains("pesu")) {
            return CommandResult.Handled("Opening Chat.", navigationTarget = "chat")
        }
        if (clean == "open create" || clean == "create" || clean == "generate" || clean.contains("create pannu")) {
            return CommandResult.Handled("Opening Studio Creation.", navigationTarget = "create")
        }
        if (clean == "open study" || clean == "study" || clean.contains("padi") || clean.contains("study assistant")) {
            return CommandResult.Handled("Opening Study Assistant.", navigationTarget = "study")
        }
        if (clean == "open memory" || clean == "remember" || clean == "memories" || clean.contains("nyabagam") || clean == "show memories") {
            return CommandResult.Handled("Opening Memory Bank.", navigationTarget = "memory")
        }
        if (clean == "open control" || clean == "control") {
            return CommandResult.Handled("Opening Control Mode.", navigationTarget = "control")
        }
        if (clean == "open history" || clean == "history") {
            return CommandResult.Handled("Opening History.", navigationTarget = "history")
        }

        // App Launching / Intents (Risk: LOW)
        if (clean.startsWith("open youtube") || clean == "youtube" || clean.contains("youtube open")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return CommandResult.Handled("Opening YouTube.", intent = intent)
        }
        if (clean.startsWith("open whatsapp") || clean == "whatsapp" || clean.contains("whatsapp open")) {
            val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            return CommandResult.Handled("Launching WhatsApp.", intent = intent)
        }
        if (clean.startsWith("open maps") || clean == "maps" || clean.contains("maps open")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=maps")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return CommandResult.Handled("Opening Maps.", intent = intent)
        }

        // Dangerous / Blocked commands (Risk: BLOCKED)
        if (clean.contains("password") || clean.contains("otp") || clean.contains("banking") || clean.contains("transfer money") || clean.contains("cvv")) {
            return CommandResult.Blocked("Security Policy: Nila is strictly blocked from automating passwords, OTP verification, or financial payments.")
        }

        // High Risk actions (Risk: HIGH)
        if (clean.contains("clear all memory") || clean.contains("delete everything") || clean.contains("wipe data")) {
            return CommandResult.NeedsConfirmation(
                actionDescription = "Wipe all local memories and chats for current profile?",
                riskLevel = RiskLevel.HIGH,
                pendingAction = { /* handled by confirmation dialog */ }
            )
        }

        return CommandResult.NotACommand
    }

    private fun normalizeMultilingualInput(raw: String): String {
        return raw.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[.,?!;]"), "")
            .replace("  ", " ")
    }
}
