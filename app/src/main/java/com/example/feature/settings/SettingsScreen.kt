package com.example.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.NetworkStatus
import com.example.core.model.OrbStyle
import com.example.core.model.ThemeMode
import com.example.core.model.VisionQuality
import com.example.core.security.UserSessionManager
import com.example.data.preferences.NilaPreferences
import com.example.data.preferences.NilaUserSettings
import com.example.ui.components.NilaGlassCard
import com.example.ui.components.NilaTopBar
import com.example.ui.theme.NilaCyan
import com.example.ui.theme.NilaEmerald
import com.example.ui.theme.NilaPurple
import com.example.ui.theme.NilaRose
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    networkStatus: NetworkStatus,
    currentSettings: NilaUserSettings,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { NilaPreferences(context) }
    val userSession = remember { UserSessionManager.getInstance(context) }
    val activeUser by userSession.currentUser.collectAsState()

    var showSignInDialog by remember { mutableStateOf(false) }
    var inputEmail by remember { mutableStateOf("") }
    var inputDisplayName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            NilaTopBar(
                title = "Settings",
                subtitle = "Multimodal & Privacy Controls",
                networkStatus = networkStatus,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile & Isolation Card
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (activeUser.isGuest) Icons.Default.PersonOutline else Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = NilaCyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeUser.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activeUser.isGuest) "Local Guest Mode (Zero Cloud Data Leakage)" else (activeUser.email ?: "Authenticated Account"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (activeUser.isGuest) {
                        Button(
                            onClick = { showSignInDialog = true },
                            modifier = Modifier.testTag("switch_to_account_button")
                        ) {
                            Text("Switch to Private Account")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { userSession.signOut() },
                            modifier = Modifier.testTag("switch_to_guest_button")
                        ) {
                            Text("Switch to Guest Mode")
                        }
                    }
                }
            }

            // Phase 2: Gestures & Optical Perception
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Vision & Gesture Interaction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hand Gesture Recognition", style = MaterialTheme.typography.bodyMedium)
                        Text("Analyze thumbs, open palm, and V-sign", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = currentSettings.isGestureControlEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch { preferences.setGestureControlEnabled(enabled) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Eye & Attention Perception", style = MaterialTheme.typography.bodyMedium)
                        Text("Nila Orb responds to device-facing attention", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = currentSettings.isEyeAttentionEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch { preferences.setEyeAttentionEnabled(enabled) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Gesture Sensitivity (${(currentSettings.gestureSensitivity * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = currentSettings.gestureSensitivity,
                    onValueChange = {
                        coroutineScope.launch { preferences.setGestureSensitivity(it) }
                    },
                    valueRange = 0.65f..0.95f,
                    steps = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Vision Processing Quality & Battery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisionQuality.values().forEach { quality ->
                        FilterChip(
                            selected = currentSettings.visionQuality == quality,
                            onClick = {
                                coroutineScope.launch { preferences.setVisionQuality(quality) }
                            },
                            label = { Text(quality.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("V-Sign Gesture Mapping", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Next", "Take Photo").forEach { action ->
                        FilterChip(
                            selected = currentSettings.vSignAction == action,
                            onClick = {
                                coroutineScope.launch { preferences.setVSignAction(action) }
                            },
                            label = { Text(action) }
                        )
                    }
                }
            }

            // Phase 2: Wake Word & Speech
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Voice & Wake Word",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wake Word ('Nila')", style = MaterialTheme.typography.bodyMedium)
                        Text("Responds with 'I am here.' when triggered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = currentSettings.isWakeWordEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch { preferences.setWakeWordEnabled(enabled) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Read Responses Aloud (TTS)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = currentSettings.isTtsEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch { preferences.setTtsEnabled(enabled) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Speech Speed (${String.format("%.1fx", currentSettings.speechRate)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = currentSettings.speechRate,
                    onValueChange = {
                        coroutineScope.launch { preferences.setSpeechRate(it) }
                    },
                    valueRange = 0.6f..1.6f,
                    steps = 4
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Speech Pitch (${String.format("%.1fx", currentSettings.speechPitch)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = currentSettings.speechPitch,
                    onValueChange = {
                        coroutineScope.launch { preferences.setSpeechPitch(it) }
                    },
                    valueRange = 0.6f..1.5f,
                    steps = 4
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Supported Voice Languages (English, Tamil, Tanglish)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("English", "Tamil", "Tanglish").forEach { lang ->
                        FilterChip(
                            selected = currentSettings.voiceLanguage == lang,
                            onClick = {
                                coroutineScope.launch { preferences.setVoiceLanguage(lang) }
                            },
                            label = { Text(lang) }
                        )
                    }
                }
            }

            // Theme Appearance
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Theme & Visual Style",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        FilterChip(
                            selected = currentSettings.themeMode == mode,
                            onClick = {
                                coroutineScope.launch { preferences.setThemeMode(mode) }
                            },
                            label = { Text(mode.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Orb Style Selection
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Nila Orb Aesthetic",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrbStyle.values().forEach { style ->
                        FilterChip(
                            selected = currentSettings.orbStyle == style,
                            onClick = {
                                coroutineScope.launch { preferences.setOrbStyle(style) }
                            },
                            label = { Text(style.displayName) }
                        )
                    }
                }
            }

            // Offline Mode & Privacy
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Privacy & Local Processing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enforce Strict Offline Mode",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Disables all external cloud network calls. All camera, gesture, OCR, and voice interactions stay strictly on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = currentSettings.isOfflineModeEnforced,
                        onCheckedChange = { enforced ->
                            coroutineScope.launch { preferences.setOfflineModeEnforced(enforced) }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reduced Motion & Low Power",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Minimizes particle animation and orb shader refresh rate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = currentSettings.isReducedMotion,
                        onCheckedChange = { reduced ->
                            coroutineScope.launch { preferences.setReducedMotion(reduced) }
                        }
                    )
                }
            }

            // About Nila AI
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "About Nila AI — Multimodal Architecture",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Nila AI integrates real on-device Google ML Kit Face Detection, real optical Hand Tracking & Gesture Recognition, local OCR Text Recognition, and native Android Speech APIs. Zero fake results or simulated detections.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Core Principles: Privacy-First • On-Device Perception • Honest Status • Multilingual (English, Tamil, Tanglish) • Never Record Secretly.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NilaCyan
                )
            }
        }
    }

    if (showSignInDialog) {
        AlertDialog(
            onDismissRequest = { showSignInDialog = false },
            title = { Text("Sign In to Private Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Separate profile datasets guarantee User A never sees User B memories, chats, or history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputDisplayName,
                        onValueChange = { inputDisplayName = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("e.g. Alex Rivera") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g. alex@example.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userSession.signInUser(inputEmail, inputDisplayName)
                        showSignInDialog = false
                    },
                    enabled = inputEmail.isNotBlank()
                ) {
                    Text("Switch Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignInDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
