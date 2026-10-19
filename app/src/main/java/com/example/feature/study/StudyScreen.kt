package com.example.feature.study

import androidx.compose.foundation.BorderStroke
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
import com.example.data.local.entity.StudyProgressEntity
import com.example.ui.components.NilaGlassCard
import com.example.ui.components.NilaTopBar
import com.example.ui.theme.NilaCyan
import com.example.ui.theme.NilaEmerald
import com.example.ui.theme.NilaPurple
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun StudyScreen(
    networkStatus: NetworkStatus,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { NilaDatabase.getInstance(context) }
    val userSession = remember { UserSessionManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var studyList by remember { mutableStateOf<List<StudyProgressEntity>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTopicTitle by remember { mutableStateOf("") }
    var newSubject by remember { mutableStateOf("Computer Science") }

    val activeUserId = userSession.currentUser.value.id

    LaunchedEffect(activeUserId) {
        db.studyDao().getStudyProgressForUser(activeUserId).collect {
            studyList = it
        }
    }

    fun addTopic() {
        if (newTopicTitle.isBlank()) return
        coroutineScope.launch {
            val item = StudyProgressEntity(
                id = UUID.randomUUID().toString(),
                userId = activeUserId,
                topic = newTopicTitle.trim(),
                subject = newSubject.trim(),
                masteryPercent = 25,
                notes = "Initial review session logged with Nila."
            )
            db.studyDao().insertStudyProgress(item)
            newTopicTitle = ""
            showAddDialog = false
        }
    }

    fun increaseMastery(item: StudyProgressEntity) {
        coroutineScope.launch {
            val updated = item.copy(
                masteryPercent = (item.masteryPercent + 25).coerceAtMost(100),
                lastStudiedAt = System.currentTimeMillis()
            )
            db.studyDao().updateStudyProgress(updated)
        }
    }

    fun deleteTopic(id: String) {
        coroutineScope.launch {
            db.studyDao().deleteStudyProgress(id, activeUserId)
        }
    }

    Scaffold(
        topBar = {
            NilaTopBar(
                title = "Study Assistant",
                subtitle = "Mastery & Knowledge Engine",
                networkStatus = networkStatus,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_study_topic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add study topic",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Study Header Card
            item {
                NilaGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderStroke = BorderStroke(1.dp, NilaPurple.copy(alpha = 0.4f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = NilaPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Personal Learning Matrix",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Track topics, review notes, and increment mastery offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (studyList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No study modules yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap + at top to add your first study subject",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(studyList, key = { it.id }) { item ->
                    NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.topic,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NilaCyan
                                )
                            }
                            IconButton(
                                onClick = { deleteTopic(item.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete topic",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mastery Progress
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Mastery: ${item.masteryPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (item.masteryPercent < 100) {
                                TextButton(
                                    onClick = { increaseMastery(item) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = NilaEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Reviewed", color = NilaEmerald)
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { item.masteryPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (item.masteryPercent >= 100) NilaEmerald else NilaPurple,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Study Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTopicTitle,
                        onValueChange = { newTopicTitle = it },
                        label = { Text("Topic name") },
                        placeholder = { Text("e.g. Jetpack Compose Canvas Shaders") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSubject,
                        onValueChange = { newSubject = it },
                        label = { Text("Subject category") },
                        placeholder = { Text("e.g. Android Architecture") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { addTopic() },
                    enabled = newTopicTitle.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
