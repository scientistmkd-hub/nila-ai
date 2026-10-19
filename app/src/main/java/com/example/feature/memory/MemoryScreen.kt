package com.example.feature.memory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.memory.MemoryRepository
import com.example.core.model.MemoryCategory
import com.example.core.model.NetworkStatus
import com.example.core.security.UserSessionManager
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.PersonProfileEntity
import com.example.data.local.entity.SavedObjectEntity
import com.example.ui.components.NilaGlassCard
import com.example.ui.components.NilaTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(
    networkStatus: NetworkStatus,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember { MemoryRepository(context) }
    val userSession = remember { UserSessionManager.getInstance(context) }
    val activeUser by userSession.currentUser.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: All, 1: People, 2: Objects, 3: Preferences, 4: Goals, 5: Study, 6: Pinned
    var searchQuery by remember { mutableStateOf("") }
    var showPrivacyDashboard by remember { mutableStateOf(false) }

    // Dialogs
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showAddObjectDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Add Memory Form State
    var memTitle by remember { mutableStateOf("") }
    var memContent by remember { mutableStateOf("") }
    var memCategory by remember { mutableStateOf(MemoryCategory.PERSONAL_INFO) }
    var memTags by remember { mutableStateOf("") }
    var memImportance by remember { mutableStateOf("NORMAL") }
    var memPinned by remember { mutableStateOf(false) }

    // Add Person Form State
    var personName by remember { mutableStateOf("") }
    var personNickname by remember { mutableStateOf("") }
    var personRel by remember { mutableStateOf("Friend") }
    var personDept by remember { mutableStateOf("") }
    var personCollege by remember { mutableStateOf("") }
    var personCourse by remember { mutableStateOf("") }
    var personYear by remember { mutableStateOf("") }
    var personIsProjectPartner by remember { mutableStateOf(false) }
    var personNotes by remember { mutableStateOf("") }

    // Add Object Form State
    var objName by remember { mutableStateOf("") }
    var objCategory by remember { mutableStateOf("Personal Item") }
    var objDesc by remember { mutableStateOf("") }
    var objNotes by remember { mutableStateOf("") }
    var objLocation by remember { mutableStateOf("") }

    // Data streams (Strictly scoped to activeUserId)
    val allMemories by repo.getMemories().collectAsStateWithLifecycle(initialValue = emptyList())
    val pinnedMemories by repo.getPinnedMemories().collectAsStateWithLifecycle(initialValue = emptyList())
    val personProfiles by repo.getPersonProfiles().collectAsStateWithLifecycle(initialValue = emptyList())
    val savedObjects by repo.getSavedObjects().collectAsStateWithLifecycle(initialValue = emptyList())

    val displayedMemories = remember(allMemories, searchQuery, activeTab) {
        val filteredBySearch = if (searchQuery.isBlank()) {
            allMemories
        } else {
            allMemories.filter {
                it.key.contains(searchQuery, ignoreCase = true) ||
                it.value.contains(searchQuery, ignoreCase = true) ||
                it.tags.contains(searchQuery, ignoreCase = true)
            }
        }

        when (activeTab) {
            0 -> filteredBySearch
            3 -> filteredBySearch.filter { it.category == MemoryCategory.USER_PREFERENCE }
            4 -> filteredBySearch.filter { it.category == MemoryCategory.GOAL }
            5 -> filteredBySearch.filter { it.category == MemoryCategory.STUDY_INFO }
            6 -> filteredBySearch.filter { it.isPinned }
            else -> filteredBySearch
        }
    }

    val displayedPeople = remember(personProfiles, searchQuery) {
        if (searchQuery.isBlank()) personProfiles
        else personProfiles.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.relationship.contains(searchQuery, ignoreCase = true) ||
            (it.department?.contains(searchQuery, ignoreCase = true) ?: false) ||
            it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    val displayedObjects = remember(savedObjects, searchQuery) {
        if (searchQuery.isBlank()) savedObjects
        else savedObjects.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.notes.contains(searchQuery, ignoreCase = true)
        }
    }

    fun exportMemoryJson() {
        coroutineScope.launch {
            val json = repo.exportUserDataJson()
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "Nila AI Memory Export")
                putExtra(Intent.EXTRA_TEXT, json)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export Nila Memory"))
            toastMessage = "Memory exported successfully."
        }
    }

    Scaffold(
        topBar = {
            NilaTopBar(
                title = "Memory Bank",
                subtitle = "Private On-Device Knowledge (${activeUser.displayName})",
                networkStatus = networkStatus,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { showPrivacyDashboard = true },
                        modifier = Modifier.testTag("memory_privacy_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Memory Privacy Dashboard",
                            tint = NilaCyan
                        )
                    }

                    IconButton(
                        onClick = {
                            when (activeTab) {
                                1 -> showAddPersonDialog = true
                                2 -> showAddObjectDialog = true
                                else -> showAddMemoryDialog = true
                            }
                        },
                        modifier = Modifier.testTag("add_memory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add memory item",
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
                .padding(horizontal = 16.dp)
        ) {
            // Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search memories, people, objects...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("memory_search_field"),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )

            // Category Tab Strip
            val tabTitles = listOf(
                "All (${allMemories.size})",
                "People (${personProfiles.size})",
                "Objects (${savedObjects.size})",
                "Preferences",
                "Goals",
                "Study",
                "Pinned (${pinnedMemories.size})"
            )

            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 8.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (activeTab) {
                1 -> {
                    // PEOPLE TAB
                    if (displayedPeople.isEmpty()) {
                        EmptyStateView(
                            title = "No Person Profiles Saved",
                            desc = "Tap + to add a private person profile (e.g. Project Partner, Classmate, Friend).",
                            onAction = { showAddPersonDialog = true },
                            actionLabel = "Add Person Profile"
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(displayedPeople, key = { it.id }) { person ->
                                PersonProfileCard(
                                    profile = person,
                                    onDelete = {
                                        coroutineScope.launch {
                                            repo.deletePersonProfile(person.id)
                                            toastMessage = "Deleted profile for ${person.name}"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // OBJECTS TAB
                    if (displayedObjects.isEmpty()) {
                        EmptyStateView(
                            title = "No Saved Objects",
                            desc = "Keep track of physical items (e.g. My Bike, Laptop, Backpack).",
                            onAction = { showAddObjectDialog = true },
                            actionLabel = "Add Saved Object"
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(displayedObjects, key = { it.id }) { obj ->
                                SavedObjectCard(
                                    item = obj,
                                    onDelete = {
                                        coroutineScope.launch {
                                            repo.deleteObject(obj.id)
                                            toastMessage = "Deleted object: ${obj.name}"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {
                    // GENERAL MEMORIES TAB
                    if (displayedMemories.isEmpty()) {
                        EmptyStateView(
                            title = "No Memories in This Category",
                            desc = "Nila only stores memories you explicitly confirm.",
                            onAction = { showAddMemoryDialog = true },
                            actionLabel = "Save a Memory"
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(displayedMemories, key = { it.id }) { memory ->
                                MemoryItemCard(
                                    item = memory,
                                    onTogglePin = {
                                        coroutineScope.launch {
                                            repo.updateMemory(memory.copy(isPinned = !memory.isPinned))
                                        }
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            repo.deleteMemory(memory.id)
                                            toastMessage = "Memory deleted"
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOGS & CONFIRMATIONS
    // ==========================================

    if (showAddMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            title = { Text("Save to Memory Bank") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Memory is transparent and private to ${activeUser.displayName}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = memTitle,
                        onValueChange = { memTitle = it },
                        label = { Text("Title / Concept") },
                        placeholder = { Text("e.g. Favorite Editor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = memContent,
                        onValueChange = { memContent = it },
                        label = { Text("Content to Remember") },
                        placeholder = { Text("e.g. Prefers Dark AMOLED mode and Kotlin DSL") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = memTags,
                        onValueChange = { memTags = it },
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("preferences, ui, android") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (memTitle.isNotBlank() && memContent.isNotBlank()) {
                            coroutineScope.launch {
                                repo.saveMemory(
                                    title = memTitle,
                                    content = memContent,
                                    category = memCategory,
                                    tags = memTags,
                                    importance = memImportance,
                                    isPinned = memPinned
                                )
                                memTitle = ""
                                memContent = ""
                                memTags = ""
                                showAddMemoryDialog = false
                                toastMessage = "Saved to Memory Bank"
                            }
                        }
                    },
                    enabled = memTitle.isNotBlank() && memContent.isNotBlank()
                ) {
                    Text("Confirm & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            title = { Text("Create Private Person Profile") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Saved profiles are strictly isolated. User B can never see your saved people.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("Full Name *") },
                        placeholder = { Text("e.g. Arun Kumar") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = personRel,
                        onValueChange = { personRel = it },
                        label = { Text("Relationship *") },
                        placeholder = { Text("Friend, Project Partner, Colleague") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = personIsProjectPartner,
                            onCheckedChange = { personIsProjectPartner = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Project Partner", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedTextField(
                        value = personDept,
                        onValueChange = { personDept = it },
                        label = { Text("Department / Field") },
                        placeholder = { Text("e.g. CSE / Robotics") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = personCollege,
                        onValueChange = { personCollege = it },
                        label = { Text("College / Organization") },
                        placeholder = { Text("e.g. MIT") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = personNotes,
                        onValueChange = { personNotes = it },
                        label = { Text("Important Notes") },
                        placeholder = { Text("Robotics project lead, prefers morning meetings") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (personName.isNotBlank() && personRel.isNotBlank()) {
                            coroutineScope.launch {
                                repo.createPersonProfile(
                                    name = personName,
                                    relationship = personRel,
                                    department = personDept,
                                    college = personCollege,
                                    course = personCourse,
                                    year = personYear,
                                    isProjectPartner = personIsProjectPartner,
                                    notes = personNotes
                                )
                                personName = ""
                                personDept = ""
                                personCollege = ""
                                personNotes = ""
                                showAddPersonDialog = false
                                toastMessage = "Saved Person Profile"
                            }
                        }
                    },
                    enabled = personName.isNotBlank() && personRel.isNotBlank()
                ) {
                    Text("Save Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPersonDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddObjectDialog) {
        AlertDialog(
            onDismissRequest = { showAddObjectDialog = false },
            title = { Text("Save Object to Memory") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = objName,
                        onValueChange = { objName = it },
                        label = { Text("Object Name *") },
                        placeholder = { Text("e.g. My Bike") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = objCategory,
                        onValueChange = { objCategory = it },
                        label = { Text("Category") },
                        placeholder = { Text("Vehicle, Electronics, Book") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = objDesc,
                        onValueChange = { objDesc = it },
                        label = { Text("Description") },
                        placeholder = { Text("Blue Yamaha R15 motorcycle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = objLocation,
                        onValueChange = { objLocation = it },
                        label = { Text("Standard Location") },
                        placeholder = { Text("e.g. Apartment Garage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = objNotes,
                        onValueChange = { objNotes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (objName.isNotBlank()) {
                            coroutineScope.launch {
                                repo.saveObject(
                                    name = objName,
                                    category = objCategory,
                                    description = objDesc,
                                    notes = objNotes,
                                    location = objLocation
                                )
                                objName = ""
                                objDesc = ""
                                objNotes = ""
                                showAddObjectDialog = false
                                toastMessage = "Object Saved"
                            }
                        }
                    },
                    enabled = objName.isNotBlank()
                ) {
                    Text("Save Object")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddObjectDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPrivacyDashboard) {
        AlertDialog(
            onDismissRequest = { showPrivacyDashboard = false },
            title = { Text("Memory Privacy & Control") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Active Profile: ${activeUser.displayName} (${if (activeUser.isGuest) "Guest Mode" else "Authenticated User"})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = NilaCyan
                    )
                    Text(
                        text = "• Memories stored: ${allMemories.size}\n• Person profiles: ${personProfiles.size}\n• Saved objects: ${savedObjects.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    Button(
                        onClick = {
                            exportMemoryJson()
                            showPrivacyDashboard = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NilaPurple)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Memories as JSON")
                    }
                    OutlinedButton(
                        onClick = {
                            showClearConfirmDialog = true
                            showPrivacyDashboard = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NilaRose)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Memories for this Profile")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDashboard = false }) { Text("Close") }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Memories?") },
            text = {
                Text("This will permanently delete all memories, person profiles, and saved objects for profile ${activeUser.displayName}. Other users will not be affected.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repo.purgeAllUserData()
                            showClearConfirmDialog = false
                            toastMessage = "All memories cleared for current profile."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NilaRose)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MemoryItemCard(
    item: MemoryEntity,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.key,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = NilaAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tags: ${item.tags}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NilaCyan
                    )
                }
            }
            IconButton(onClick = onTogglePin, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (item.isPinned) Icons.Default.PushPin else Icons.Default.BookmarkBorder,
                    contentDescription = "Pin memory",
                    tint = if (item.isPinned) NilaAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete memory",
                    tint = NilaRose.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PersonProfileCard(
    profile: PersonProfileEntity,
    onDelete: () -> Unit
) {
    NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(NilaCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NilaCyan
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (profile.isProjectPartner) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NilaPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Project Partner",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = NilaPurple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = profile.relationship + if (!profile.department.isNullOrBlank()) " • ${profile.department}" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = NilaCyan
                )
                if (profile.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete profile",
                    tint = NilaRose.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SavedObjectCard(
    item: SavedObjectEntity,
    onDelete: () -> Unit
) {
    NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(NilaEmerald.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = NilaEmerald,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.category + if (item.location.isNotBlank()) " • ${item.location}" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = NilaEmerald
                )
                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete object",
                    tint = NilaRose.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    title: String,
    desc: String,
    onAction: () -> Unit,
    actionLabel: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(actionLabel)
            }
        }
    }
}
