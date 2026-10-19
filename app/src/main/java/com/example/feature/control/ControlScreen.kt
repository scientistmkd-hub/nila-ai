package com.example.feature.control

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.NetworkStatus
import com.example.core.model.RiskLevel
import com.example.core.security.UserSessionManager
import com.example.data.preferences.NilaPreferences
import com.example.ui.components.NilaGlassCard
import com.example.ui.components.NilaTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ControlScreen(
    networkStatus: NetworkStatus,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { NilaPreferences(context) }
    val userSession = remember { UserSessionManager.getInstance(context) }

    var isControlModeEnabled by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingConfirmationAction by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        preferences.settingsFlow.collect {
            isControlModeEnabled = it.isControlModeEnabled
        }
    }

    fun launchIntent(intent: Intent, actionName: String) {
        try {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            statusMessage = "Executed: $actionName"
            userSession.logAuditEvent("CONTROL_INTENT_EXECUTE", "Launched $actionName")
        } catch (e: Exception) {
            statusMessage = "Could not open target application: ${e.localizedMessage}"
        }
    }

    Scaffold(
        topBar = {
            NilaTopBar(
                title = "Control Mode",
                subtitle = "Safe Device Automation",
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
                .padding(16.dp)
        ) {
            // Master Switch Card
            NilaGlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderStroke = BorderStroke(1.dp, if (isControlModeEnabled) NilaCyan.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Control Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Allows Nila to trigger app shortcuts and standard intent actions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isControlModeEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                preferences.setControlModeEnabled(enabled)
                                userSession.logAuditEvent(
                                    if (enabled) "CONTROL_MODE_ENABLED" else "CONTROL_MODE_DISABLED",
                                    "Control Mode switched to $enabled"
                                )
                            }
                        },
                        modifier = Modifier.testTag("control_mode_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Safety Policy Card
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = NilaEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Safety Architecture & Risk Levels",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                RiskLevelItem("LOW", "Standard apps, navigation, viewing intents", NilaEmerald)
                RiskLevelItem("MEDIUM", "Sending messages, setting alarms (Confirmation required)", NilaAmber)
                RiskLevelItem("HIGH", "Clearing databases, wiping history (Explicit authorization)", NilaRose)
                RiskLevelItem("BLOCKED", "Passwords, OTP codes, Banking and payment confirmations", Color.Red)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emergency Stop Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        preferences.setControlModeEnabled(false)
                        statusMessage = "EMERGENCY STOP ENGAGED: All automation suspended."
                        userSession.logAuditEvent("EMERGENCY_STOP", "Emergency stop triggered by user.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("emergency_stop_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NilaRose),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emergency Stop / Suspend Automation", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Safe App Launch Shortcuts (LOW Risk)
            Text(
                text = "Safe Application Shortcuts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionRow(
                    title = "System Settings",
                    subtitle = "Open Android device configuration",
                    icon = Icons.Default.Settings,
                    enabled = isControlModeEnabled,
                    onClick = {
                        launchIntent(Intent(Settings.ACTION_SETTINGS), "System Settings")
                    }
                )
                ActionRow(
                    title = "YouTube",
                    subtitle = "Browse educational videos & content",
                    icon = Icons.Default.PlayCircle,
                    enabled = isControlModeEnabled,
                    onClick = {
                        launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")), "YouTube")
                    }
                )
                ActionRow(
                    title = "WhatsApp",
                    subtitle = "Open messaging client",
                    icon = Icons.Default.Chat,
                    enabled = isControlModeEnabled,
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com"))
                        launchIntent(intent, "WhatsApp")
                    }
                )
                ActionRow(
                    title = "Maps Navigation",
                    subtitle = "Open local map view",
                    icon = Icons.Default.Map,
                    enabled = isControlModeEnabled,
                    onClick = {
                        launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=directions")), "Maps")
                    }
                )
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = statusMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = NilaCyan,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RiskLevelItem(level: String, desc: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = color.copy(alpha = 0.2f),
            modifier = Modifier.width(72.dp)
        ) {
            Text(
                text = level,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    NilaGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}
