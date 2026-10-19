package com.example.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.core.security.UserSessionManager
import com.example.data.local.NilaDatabase
import com.example.data.local.entity.AuditEventEntity
import com.example.data.local.entity.GenerationHistoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.ui.components.NilaGlassCard
import com.example.ui.components.NilaTopBar
import com.example.ui.theme.NilaCyan
import com.example.ui.theme.NilaEmerald
import com.example.ui.theme.NilaRose
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    networkStatus: NetworkStatus,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { NilaDatabase.getInstance(context) }
    val userSession = remember { UserSessionManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: Messages, 1: Generations, 2: Security Audit
    var messages by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    var generations by remember { mutableStateOf<List<GenerationHistoryEntity>>(emptyList()) }
    var auditEvents by remember { mutableStateOf<List<AuditEventEntity>>(emptyList()) }

    val activeUserId = userSession.currentUser.value.id
    val timeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    LaunchedEffect(activeUserId) {
        db.messageDao().getRecentMessagesForUser(activeUserId).collect {
            messages = it
        }
    }

    LaunchedEffect(activeUserId) {
        db.generationDao().getGenerationsForUser(activeUserId).collect {
            generations = it
        }
    }

    LaunchedEffect(activeUserId) {
        db.auditDao().getAuditEventsForUser(activeUserId).collect {
            auditEvents = it
        }
    }

    Scaffold(
        topBar = {
            NilaTopBar(
                title = "History & Audit",
                subtitle = "User-Scoped Event Log",
                networkStatus = networkStatus,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                db.auditDao().clearAuditEventsForUser(activeUserId)
                            }
                        },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear audit history",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                .padding(horizontal = 16.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Chats (${messages.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Studio (${generations.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Audit (${auditEvents.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    if (messages.isEmpty()) {
                        EmptyStateText("No chat messages recorded for this profile.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = msg.role.name,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (msg.role.name == "USER") MaterialTheme.colorScheme.secondary else NilaCyan
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = timeFormat.format(Date(msg.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (generations.isEmpty()) {
                        EmptyStateText("No creation history recorded for this profile.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(generations, key = { it.id }) { gen ->
                                NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${gen.type} • ${gen.style}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NilaEmerald
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = timeFormat.format(Date(gen.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = gen.prompt,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    if (gen.resultInfo.isNotBlank()) {
                                        Text(
                                            text = gen.resultInfo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (auditEvents.isEmpty()) {
                        EmptyStateText("No security audit events recorded.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(auditEvents, key = { it.id }) { audit ->
                                NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = NilaCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = audit.eventType,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NilaCyan
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = timeFormat.format(Date(audit.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = audit.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
