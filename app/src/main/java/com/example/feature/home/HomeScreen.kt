package com.example.feature.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.AiRouter
import com.example.ai.voice.AndroidSpeechRecognizerEngine
import com.example.ai.voice.AndroidTtsEngine
import com.example.ai.voice.AndroidWakeWordEngine
import com.example.core.command.CommandResult
import com.example.core.command.LocalCommandRouter
import com.example.core.model.NetworkStatus
import com.example.core.model.OrbState
import com.example.core.model.VoiceState
import com.example.core.security.UserSessionManager
import com.example.data.preferences.NilaUserSettings
import com.example.ui.components.NilaGlassCard
import com.example.ui.components.NilaOrb
import com.example.ui.components.NilaTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    networkStatus: NetworkStatus,
    settings: NilaUserSettings,
    onNavigate: (String) -> Unit,
    userSessionManager: UserSessionManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val localCommandRouter = remember { LocalCommandRouter(context) }
    val aiRouter = remember { AiRouter(context, localCommandRouter) }
    val ttsEngine = remember { AndroidTtsEngine(context) }
    val speechEngine = remember { AndroidSpeechRecognizerEngine(context) }
    val wakeWordEngine = remember { AndroidWakeWordEngine(context) }

    var orbState by remember { mutableStateOf(OrbState.IDLE) }
    var activeStatusText by remember { mutableStateOf("Tap Nila or microphone to interact") }
    var lastResponseText by remember { mutableStateOf<String?>(null) }
    var partialTranscript by remember { mutableStateOf<String?>(null) }

    val voiceState by speechEngine.voiceState.collectAsStateWithLifecycle()
    val isSpeaking by ttsEngine.isSpeaking.collectAsStateWithLifecycle()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun executeSpeechResult(spokenText: String) {
        partialTranscript = null
        val cmdResult = localCommandRouter.evaluate(spokenText)

        when (cmdResult) {
            is CommandResult.Handled -> {
                if (cmdResult.shouldStopSpeech) {
                    ttsEngine.stop()
                    speechEngine.stopListening()
                    orbState = OrbState.IDLE
                    activeStatusText = "Speech stopped"
                    return
                }

                if (cmdResult.navigationTarget != null) {
                    onNavigate(cmdResult.navigationTarget)
                    return
                }

                if (cmdResult.intent != null) {
                    try {
                        context.startActivity(cmdResult.intent)
                        activeStatusText = cmdResult.reply
                    } catch (e: Exception) {
                        activeStatusText = "Could not open target app"
                    }
                    return
                }

                // Normal handled text
                coroutineScope.launch {
                    lastResponseText = cmdResult.reply
                    activeStatusText = "Answered locally"
                    orbState = OrbState.SPEAKING
                    if (settings.isTtsEnabled) {
                        ttsEngine.speak(
                            cmdResult.reply,
                            rate = settings.speechRate,
                            pitch = settings.speechPitch,
                            languageName = settings.voiceLanguage
                        )
                    }
                }
            }
            is CommandResult.Blocked -> {
                lastResponseText = cmdResult.reason
                activeStatusText = "Blocked by security policy"
                orbState = OrbState.ERROR
            }
            is CommandResult.NeedsConfirmation -> {
                lastResponseText = "⚠️ Action needs confirmation: ${cmdResult.actionDescription}"
                activeStatusText = "Safety verification required"
            }
            is CommandResult.NotACommand -> {
                // Route to AI router
                coroutineScope.launch {
                    orbState = OrbState.THINKING
                    activeStatusText = "Processing: \"$spokenText\""

                    val (reply, provider) = aiRouter.routeQuery(
                        prompt = spokenText,
                        isOfflineEnforced = settings.isOfflineModeEnforced || networkStatus == NetworkStatus.OFFLINE
                    )

                    lastResponseText = reply
                    activeStatusText = "Answered via $provider"
                    orbState = OrbState.SPEAKING

                    if (settings.isTtsEnabled) {
                        ttsEngine.speak(
                            reply,
                            rate = settings.speechRate,
                            pitch = settings.speechPitch,
                            languageName = settings.voiceLanguage
                        )
                    }
                }
            }
        }
    }

    fun resumeWakeWordIfNeeded() {
        if (settings.isWakeWordEnabled && hasAudioPermission) {
            wakeWordEngine.startListening(
                onWakeWordDetected = {
                    coroutineScope.launch { executeSpeechResult("Nila") }
                }
            )
        }
    }

    // Wake word architecture ("Nila" -> "I am here.")
    LaunchedEffect(settings.isWakeWordEnabled, hasAudioPermission) {
        if (settings.isWakeWordEnabled && hasAudioPermission) {
            wakeWordEngine.startListening(
                onWakeWordDetected = {
                    coroutineScope.launch {
                        executeSpeechResult("Nila")
                    }
                },
                onError = {
                    // Non-fatal, manual mic button remains always active
                }
            )
        } else {
            wakeWordEngine.stopListening()
        }
    }

    // Update orb state if offline or speaking
    LaunchedEffect(networkStatus, settings.isOfflineModeEnforced, isSpeaking, voiceState) {
        when {
            isSpeaking -> orbState = OrbState.SPEAKING
            voiceState == VoiceState.LISTENING -> orbState = OrbState.LISTENING
            voiceState == VoiceState.PROCESSING -> orbState = OrbState.THINKING
            networkStatus == NetworkStatus.OFFLINE || settings.isOfflineModeEnforced -> {
                orbState = OrbState.OFFLINE
                if (activeStatusText == "Nila is ready") {
                    activeStatusText = "Operating in Local-Only Offline Mode"
                }
            }
            orbState == OrbState.OFFLINE -> orbState = OrbState.IDLE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            wakeWordEngine.release()
            ttsEngine.shutdown()
            speechEngine.release()
        }
    }

    fun startListeningInternal() {
        wakeWordEngine.stopListening()
        if (isSpeaking) {
            ttsEngine.stop()
        }
        speechEngine.startListening(
            language = settings.voiceLanguage,
            onPartialResult = { partial ->
                partialTranscript = partial
                activeStatusText = "Hearing: $partial"
            },
            onFinalResult = { finalSpoken ->
                if (finalSpoken.isNotBlank()) {
                    executeSpeechResult(finalSpoken)
                } else {
                    activeStatusText = "No speech detected"
                    orbState = OrbState.IDLE
                }
                resumeWakeWordIfNeeded()
            },
            onError = { err ->
                activeStatusText = err
                orbState = OrbState.ERROR
                coroutineScope.launch {
                    kotlinx.coroutines.delay(2000)
                    orbState = if (networkStatus == NetworkStatus.OFFLINE) OrbState.OFFLINE else OrbState.IDLE
                    resumeWakeWordIfNeeded()
                }
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            startListeningInternal()
        } else {
            activeStatusText = "Microphone permission is required for voice commands."
            orbState = OrbState.ERROR
        }
    }

    fun onMicTapped() {
        if (voiceState == VoiceState.LISTENING) {
            speechEngine.stopListening()
            orbState = OrbState.IDLE
            activeStatusText = "Listening cancelled"
            resumeWakeWordIfNeeded()
        } else if (isSpeaking) {
            ttsEngine.stop()
            orbState = OrbState.IDLE
            activeStatusText = "Speech interrupted"
        } else if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startListeningInternal()
        }
    }

    Scaffold(
        topBar = {
            NilaTopBar(
                title = "Nila AI",
                subtitle = "Multimodal Personal Assistant",
                networkStatus = networkStatus,
                orbState = orbState,
                actions = {
                    if (isSpeaking) {
                        IconButton(
                            onClick = {
                                ttsEngine.stop()
                                orbState = OrbState.IDLE
                            },
                            modifier = Modifier.testTag("home_stop_speech_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = "Interrupt speech",
                                tint = NilaRose
                            )
                        }
                    }
                    IconButton(
                        onClick = { onNavigate("settings") },
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Assistant settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Central Interactive Orb
            NilaOrb(
                orbState = orbState,
                orbStyle = settings.orbStyle,
                size = 230.dp,
                onClick = {
                    if (isSpeaking) {
                        ttsEngine.stop()
                        orbState = OrbState.IDLE
                        activeStatusText = "Speech stopped"
                    } else if (voiceState == VoiceState.LISTENING) {
                        speechEngine.stopListening()
                        orbState = OrbState.IDLE
                    } else {
                        // Wake word invocation: "Nila"
                        executeSpeechResult("Nila")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // State & Prompt feedback pill
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Text(
                    text = partialTranscript ?: activeStatusText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            AnimatedVisibility(
                visible = lastResponseText != null,
                enter = fadeIn() + expandVertically()
            ) {
                lastResponseText?.let { reply ->
                    NilaGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "Nila",
                                tint = NilaCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nila",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = NilaCyan
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    if (isSpeaking) {
                                        ttsEngine.stop()
                                    } else {
                                        ttsEngine.speak(
                                            reply,
                                            rate = settings.speechRate,
                                            pitch = settings.speechPitch,
                                            languageName = settings.voiceLanguage
                                        )
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Speak response",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = reply,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Microphone Quick Action Fab
            FilledIconButton(
                onClick = { onMicTapped() },
                modifier = Modifier
                    .size(68.dp)
                    .testTag("home_mic_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = when {
                        voiceState == VoiceState.LISTENING -> NilaEmerald
                        isSpeaking -> NilaRose
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = when {
                        isSpeaking -> Icons.Default.Stop
                        voiceState == VoiceState.LISTENING -> Icons.Default.GraphicEq
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Voice input",
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Capabilities",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigate("chat") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Actions Grid (6 primary capabilities)
            val actions = listOf(
                QuickActionItem("chat", "Ask Nila", "Conversational AI & memory", Icons.Default.Chat, NilaCyan),
                QuickActionItem("camera", "Camera Vision", "Real-time perception & gestures", Icons.Default.CameraAlt, NilaPink),
                QuickActionItem("create", "Create Studio", "Image & 3D generation", Icons.Default.AutoAwesome, NilaPurple),
                QuickActionItem("study", "Study Assistant", "Flashcards & document summary", Icons.Default.School, NilaViolet),
                QuickActionItem("memory", "Memory Bank", "Private profiles & saved facts", Icons.Default.Psychology, NilaEmerald),
                QuickActionItem("control", "Control Mode", "Device automation & safety", Icons.Default.TouchApp, NilaCyan)
            )

            // 2 column layout
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in actions.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            item = actions[i],
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(actions[i].route) }
                        )
                        if (i + 1 < actions.size) {
                            QuickActionCard(
                                item = actions[i + 1],
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(actions[i + 1].route) }
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private data class QuickActionItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tintColor: Color
)

@Composable
private fun QuickActionCard(
    item: QuickActionItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NilaGlassCard(
        modifier = modifier
            .testTag("quick_action_${item.route}"),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(item.tintColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.tintColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}
