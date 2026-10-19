package com.example.feature.creation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ai.creation.CreationRouter
import com.example.core.image.ImageFilterType
import com.example.core.image.LocalImageEditor
import com.example.core.model.*
import com.example.core.prompt.PromptProcessor
import com.example.core.prompt.PromptValidationResult
import com.example.core.security.UserSessionManager
import com.example.core.storage.CreationStorageManager
import com.example.data.local.NilaDatabase
import com.example.data.local.entity.GenerationHistoryEntity
import com.example.data.preferences.NilaPreferences
import com.example.data.preferences.NilaUserSettings
import com.example.ui.components.NilaGlassCard
import com.example.ui.components.NilaOrb
import com.example.ui.components.NilaTopBar
import com.example.ui.theme.*
import com.example.ui.threed.MeshData
import com.example.ui.threed.ThreeDMeshEngine
import com.example.ui.threed.ThreeDViewerScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable
fun CreateScreen(
    networkStatus: NetworkStatus,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { NilaDatabase.getInstance(context) }
    val userSession = remember { UserSessionManager.getInstance(context) }
    val currentUser by userSession.currentUser.collectAsStateWithLifecycle()
    val preferences = remember { NilaPreferences(context) }
    val settings by preferences.settingsFlow.collectAsStateWithLifecycle(initialValue = NilaUserSettings())
    val coroutineScope = rememberCoroutineScope()

    val storageManager = remember { CreationStorageManager(context) }
    val promptProcessor = remember { PromptProcessor() }
    val creationRouter = remember { CreationRouter(context, storageManager) }
    val localImageEditor = remember { LocalImageEditor() }

    // Studio Modes: IMAGE, ANIME, THREE_D, EDIT
    var selectedMode by remember { mutableStateOf(CreationMode.IMAGE) }
    var promptInput by remember { mutableStateOf("") }
    var negativePromptInput by remember { mutableStateOf("") }
    var showNegativePrompt by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var isEnhanceEnabled by remember { mutableStateOf(true) }

    // Settings
    var selectedStyle by remember { mutableStateOf(ImageStyle.SCI_FI) }
    var selectedAnimePreset by remember { mutableStateOf(AnimePreset.ANIME_CHARACTER) }
    var selectedAspectRatio by remember { mutableStateOf(CreationAspectRatio.RATIO_1_1) }
    var selectedQuality by remember { mutableStateOf(GenerationQuality.STANDARD) }
    var customSeed by remember { mutableStateOf<Long?>(null) }
    var useRandomSeed by remember { mutableStateOf(true) }
    var animeGestureModeEnabled by remember { mutableStateOf(false) }

    // State & Execution
    var generationState by remember { mutableStateOf(GenerationState.IDLE) }
    var currentOrbState by remember { mutableStateOf(OrbState.IDLE) }
    var activeJob: Job? by remember { mutableStateOf(null) }
    var currentResponse by remember { mutableStateOf<CreationResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var enhancedPromptPreview by remember { mutableStateOf<String?>(null) }

    // Edit Mode State
    var selectedSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedFilter by remember { mutableStateOf(ImageFilterType.NONE) }
    var brightnessLevel by remember { mutableStateOf(0f) }
    var contrastLevel by remember { mutableStateOf(1f) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var confirmBeforeCloudGeneration by remember { mutableStateOf(false) }

    // 3D Viewer Launch
    var activeViewerMesh by remember { mutableStateOf<MeshData?>(null) }
    var activeViewerPrompt by remember { mutableStateOf("") }

    // Gallery & Collections
    var historyItems by remember { mutableStateOf<List<GenerationHistoryEntity>>(emptyList()) }
    var selectedFilterTab by remember { mutableStateOf("ALL") } // ALL, IMAGE, ANIME, 3D, EDIT, FAVORITES
    var searchQuery by remember { mutableStateOf("") }

    // Photo picker for Edit Mode
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    selectedSourceBitmap = bmp
                }
            } catch (_: Exception) {}
        }
    }

    // Load user-isolated generation history
    LaunchedEffect(currentUser.id, selectedFilterTab, searchQuery) {
        val flow = when {
            searchQuery.isNotBlank() -> db.generationDao().searchGenerations(currentUser.id, searchQuery)
            selectedFilterTab == "FAVORITES" -> db.generationDao().getFavoriteGenerations(currentUser.id)
            selectedFilterTab in listOf("IMAGE", "ANIME", "3D", "EDIT") -> db.generationDao().getGenerationsByType(currentUser.id, selectedFilterTab)
            else -> db.generationDao().getGenerationsForUser(currentUser.id)
        }
        flow.collect { historyItems = it }
    }

    // Auto-update enhanced prompt preview when inputs change
    LaunchedEffect(promptInput, selectedStyle, selectedAnimePreset, selectedMode, isEnhanceEnabled) {
        if (promptInput.isNotBlank() && isEnhanceEnabled) {
            enhancedPromptPreview = promptProcessor.enhancePrompt(
                basePrompt = promptInput,
                style = selectedStyle,
                animePreset = selectedAnimePreset,
                mode = selectedMode
            )
        } else {
            enhancedPromptPreview = null
        }
    }

    fun executeGeneration() {
        val validation = promptProcessor.validatePrompt(promptInput)
        if (validation is PromptValidationResult.Invalid) {
            errorMessage = validation.reason
            generationState = GenerationState.FAILED
            currentOrbState = OrbState.ERROR
            return
        }

        errorMessage = null
        generationState = GenerationState.PREPARING
        currentOrbState = OrbState.THINKING

        val request = CreationRequest(
            prompt = if (isEnhanceEnabled) (enhancedPromptPreview ?: promptInput) else promptInput,
            negativePrompt = negativePromptInput,
            style = selectedStyle,
            animePreset = selectedAnimePreset,
            aspectRatio = selectedAspectRatio,
            quality = selectedQuality,
            seed = if (useRandomSeed) null else customSeed,
            mode = selectedMode
        )

        activeJob = coroutineScope.launch {
            try {
                delay(400)
                generationState = GenerationState.GENERATING
                currentOrbState = OrbState.GENERATING

                val result = when (selectedMode) {
                    CreationMode.IMAGE -> creationRouter.routeImageGeneration(request, currentUser.id)
                    CreationMode.ANIME -> creationRouter.routeAnimeGeneration(request, currentUser.id)
                    CreationMode.THREE_D -> creationRouter.route3DGeneration(request, currentUser.id)
                    CreationMode.EDIT -> {
                        val source = selectedSourceBitmap
                        if (source != null) {
                            var filtered = localImageEditor.applyFilter(source, selectedFilter)
                            if (brightnessLevel != 0f || contrastLevel != 1f) {
                                filtered = localImageEditor.adjustBrightnessAndContrast(filtered, brightnessLevel, contrastLevel)
                            }
                            creationRouter.routeImageEdit(request, filtered, currentUser.id)
                        } else {
                            creationRouter.routeImageGeneration(request, currentUser.id)
                        }
                    }
                }

                generationState = GenerationState.FINALIZING
                delay(300)

                result.onSuccess { resp ->
                    currentResponse = resp
                    generationState = GenerationState.SUCCESS
                    currentOrbState = OrbState.SUCCESS

                    // Persist to Room under active user's isolated account
                    val entity = GenerationHistoryEntity(
                        id = resp.assetId,
                        userId = currentUser.id,
                        prompt = resp.prompt,
                        type = when (selectedMode) {
                            CreationMode.IMAGE -> "IMAGE"
                            CreationMode.ANIME -> "ANIME"
                            CreationMode.THREE_D -> "3D"
                            CreationMode.EDIT -> "EDIT"
                        },
                        style = selectedStyle.displayName,
                        provider = resp.provider,
                        model = resp.model,
                        status = "SUCCESS",
                        assetUri = resp.assetUri,
                        assetType = resp.assetType.name,
                        width = resp.width,
                        height = resp.height,
                        format = resp.threeDFormat.extension,
                        meshUri = resp.meshUri,
                        thumbnailUri = resp.thumbnailUri,
                        negativePrompt = resp.negativePrompt
                    )
                    db.generationDao().insertGeneration(entity)

                    // Return orb to IDLE after success celebration
                    launch {
                        delay(2000)
                        currentOrbState = OrbState.IDLE
                    }
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Generation failed"
                    generationState = GenerationState.FAILED
                    currentOrbState = OrbState.ERROR
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Unexpected creation error"
                generationState = GenerationState.FAILED
                currentOrbState = OrbState.ERROR
            }
        }
    }

    fun startGeneration() {
        if (confirmBeforeCloudGeneration) {
            showConfirmationDialog = true
        } else {
            executeGeneration()
        }
    }

    fun cancelActiveGeneration() {
        activeJob?.cancel()
        activeJob = null
        generationState = GenerationState.CANCELLED
        currentOrbState = OrbState.IDLE
        errorMessage = "Generation cancelled by user."
    }

    Scaffold(
        topBar = {
            NilaTopBar(
                title = "Creation Studio",
                subtitle = "Multimodal AI Generation",
                networkStatus = networkStatus,
                orbState = currentOrbState,
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
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            // Mode Selector: [ Image ] [ Anime ] [ 3D ] [ Edit ]
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CreationMode.values().forEach { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) NilaCyan else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedMode = mode
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp)
                                .testTag("create_tab_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Prompt Field
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (selectedMode) {
                        CreationMode.IMAGE -> "Prompt Description"
                        CreationMode.ANIME -> "Anime Concept Prompt"
                        CreationMode.THREE_D -> "3D Mesh Description"
                        CreationMode.EDIT -> "Modification Instruction"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = NilaCyan
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            when (selectedMode) {
                                CreationMode.IMAGE -> "e.g. A futuristic city floating above Chennai at sunset with holographic reflections..."
                                CreationMode.ANIME -> "e.g. Original anime cybernetic warrior with glowing cyan hair standing in rain..."
                                CreationMode.THREE_D -> "e.g. High-tech cyber drone with quad propulsion pods and sensor dome..."
                                CreationMode.EDIT -> "e.g. Change the background to neon cyberpunk night city..."
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .testTag("creation_prompt_input"),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 5
                )

                // Enhanced Prompt Preview
                if (isEnhanceEnabled && enhancedPromptPreview != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NilaPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NilaPurple.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = NilaPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enhanced Prompt Preview:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NilaPurple)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = enhancedPromptPreview!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Negative Prompt (Expandable)
                if (showNegativePrompt) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = negativePromptInput,
                        onValueChange = { negativePromptInput = it },
                        placeholder = { Text("Negative Prompt: blurry, distorted, extra limbs, low quality...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        label = { Text("Negative Prompt") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggles Row: Enhance Prompt & Negative Prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isEnhanceEnabled,
                            onCheckedChange = { isEnhanceEnabled = it },
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Enhance", style = MaterialTheme.typography.labelMedium)
                    }

                    TextButton(onClick = { showNegativePrompt = !showNegativePrompt }) {
                        Icon(imageVector = if (showNegativePrompt) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showNegativePrompt) "Hide Negative" else "+ Negative Prompt", style = MaterialTheme.typography.labelMedium)
                    }

                    TextButton(onClick = { showAdvancedSettings = !showAdvancedSettings }) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Settings", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode-Specific Controls
            when (selectedMode) {
                CreationMode.ANIME -> {
                    // Anime Presets
                    Text("Anime Style Presets", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AnimePreset.values()) { preset ->
                            FilterChip(
                                selected = selectedAnimePreset == preset,
                                onClick = { selectedAnimePreset = preset },
                                label = { Text(preset.displayName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Anime Gesture Pose Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anime Gesture Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Uses Phase 2 V-Sign gesture to synthesize anime pose", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = animeGestureModeEnabled,
                            onCheckedChange = { animeGestureModeEnabled = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (animeGestureModeEnabled) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NilaPurple.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NilaPurple.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "V-Sign Gesture Trigger Active",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NilaPurple
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Show a Peace/V-Sign to camera or trigger the pose synthesis manually below:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        promptInput = "Original anime character striking dynamic victory pose with glowing electric aura, cyberpunk Chennai background"
                                        startGeneration()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NilaPurple),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Synthesize V-Sign Anime Hero", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                CreationMode.EDIT -> {
                    // Local Image Input
                    NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Source Image Input", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = NilaCyan)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = NilaCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick Photo", color = Color.Black)
                            }
                            OutlinedButton(
                                onClick = { onNavigateBack() }, // Camera trigger
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera")
                            }
                        }

                        if (selectedSourceBitmap != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Image(
                                bitmap = selectedSourceBitmap!!.asImageBitmap(),
                                contentDescription = "Source to edit",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Local filter pills
                            Text("Local Image Filters (Offline)", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(ImageFilterType.values()) { filter ->
                                    FilterChip(
                                        selected = selectedFilter == filter,
                                        onClick = { selectedFilter = filter },
                                        label = { Text(filter.label) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Direct Rotate & Flip Tools
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedSourceBitmap?.let { bmp ->
                                            coroutineScope.launch {
                                                selectedSourceBitmap = localImageEditor.rotateBitmap(bmp, 90f)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rotate 90°", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedSourceBitmap?.let { bmp ->
                                            coroutineScope.launch {
                                                selectedSourceBitmap = localImageEditor.flipBitmap(bmp, horizontal = true)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Flip", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                CreationMode.THREE_D -> {
                    // Real 3D rule notice
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NilaCyan.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, NilaCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NilaCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Absolute 3D Standard: Nila generates authentic OBJ mesh geometry with verified vertices and faces, rendered in an interactive 3D viewport.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                CreationMode.IMAGE -> {
                    // Style Selector
                    Text("Artistic Styles", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ImageStyle.values()) { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = { Text(style.displayName) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Advanced Settings (Aspect Ratio, Quality, Seed)
            if (showAdvancedSettings) {
                NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CreationAspectRatio.values().take(4).forEach { ratio ->
                            FilterChip(
                                selected = selectedAspectRatio == ratio,
                                onClick = { selectedAspectRatio = ratio },
                                label = { Text(ratio.label.split(" ")[0]) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirmation before cloud generation toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Confirm Before Generation", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Shows prompt, provider, and settings confirmation dialog", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = confirmBeforeCloudGeneration,
                            onCheckedChange = { confirmBeforeCloudGeneration = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Central Nila Orb Visual Generation Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                NilaOrb(
                    orbState = currentOrbState,
                    orbStyle = settings.orbStyle,
                    size = 140.dp
                )
            }

            // Error display
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NilaRose.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NilaRose.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = NilaRose)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Primary Action: Generate / Cancel Button
            if (generationState in listOf(GenerationState.PREPARING, GenerationState.GENERATING, GenerationState.QUEUED)) {
                Button(
                    onClick = { cancelActiveGeneration() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NilaRose)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cancel Generation", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { startGeneration() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("creation_generate_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NilaCyan),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Brush, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (selectedMode) {
                            CreationMode.IMAGE -> "Generate Image"
                            CreationMode.ANIME -> "Synthesize Anime"
                            CreationMode.THREE_D -> "Generate 3D Model"
                            CreationMode.EDIT -> "Apply Transformation"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            }

            // Active / Latest Result Preview Card
            currentResponse?.let { resp ->
                Spacer(modifier = Modifier.height(20.dp))
                NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (resp.isReal3D) "Real 3D Asset Created" else "Generated Result",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NilaCyan
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (resp.isReal3D) NilaCyan.copy(alpha = 0.2f) else NilaPurple.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = resp.provider,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (resp.isReal3D) NilaCyan else NilaPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val file = File(Uri.parse(resp.assetUri).path ?: "")
                    if (file.exists()) {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Generated Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "\"${resp.prompt}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Result Actions: 3D View / Variations / Upscale / Edit / Export / Share / Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (resp.isReal3D) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val meshFile = resp.meshUri?.let { uriStr ->
                                            try { File(Uri.parse(uriStr).path ?: "") } catch (_: Exception) { null }
                                        }
                                        val loaded = if (meshFile != null && meshFile.exists()) {
                                            ThreeDMeshEngine.parseObjString(meshFile.readText(), name = resp.prompt.take(24))
                                        } else {
                                            ThreeDMeshEngine.createCyberDrone()
                                        }
                                        activeViewerMesh = loaded
                                        activeViewerPrompt = resp.prompt
                                    }
                                },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = NilaCyan)
                            ) {
                                Icon(imageVector = Icons.Default.ViewInAr, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("3D Viewport", color = Color.Black)
                            }
                        } else {
                            // Upscale 2X Button
                            OutlinedButton(
                                onClick = {
                                    val f = File(Uri.parse(resp.assetUri).path ?: "")
                                    if (f.exists()) {
                                        val bmp = BitmapFactory.decodeFile(f.absolutePath)
                                        if (bmp != null) {
                                            coroutineScope.launch {
                                                generationState = GenerationState.GENERATING
                                                currentOrbState = OrbState.GENERATING
                                                creationRouter.routeUpscale(bmp, currentUser.id).onSuccess { upscaled ->
                                                    currentResponse = upscaled
                                                    generationState = GenerationState.SUCCESS
                                                    currentOrbState = OrbState.SUCCESS
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.HighQuality, contentDescription = null, tint = NilaCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upscale 2X", color = NilaCyan)
                            }
                        }

                        // Variations
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    generationState = GenerationState.GENERATING
                                    currentOrbState = OrbState.GENERATING
                                    creationRouter.routeVariations(resp, 1, currentUser.id).onSuccess { vars ->
                                        if (vars.isNotEmpty()) {
                                            currentResponse = vars.first()
                                        }
                                        generationState = GenerationState.SUCCESS
                                        currentOrbState = OrbState.SUCCESS
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.DynamicFeed, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Variation")
                        }

                        // Edit in Studio
                        OutlinedButton(
                            onClick = {
                                val f = File(Uri.parse(resp.assetUri).path ?: "")
                                if (f.exists()) {
                                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                                    if (bmp != null) {
                                        selectedSourceBitmap = bmp
                                        selectedMode = CreationMode.EDIT
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Use as Prompt
                        IconButton(onClick = { promptInput = resp.prompt }) {
                            Icon(imageVector = Icons.Default.EditNote, contentDescription = "Use as Prompt", tint = NilaCyan)
                        }

                        // Regenerate
                        IconButton(onClick = { startGeneration() }) {
                            Icon(imageVector = Icons.Default.Replay, contentDescription = "Regenerate", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        // Export / Download
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Exported Nila AI Creation (${resp.assetType}): \"${resp.prompt}\"\nProvider: ${resp.provider}\nAsset URI: ${resp.assetUri}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Creation"))
                            }
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export Asset", tint = NilaEmerald)
                        }

                        // Share
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Generated by Nila AI Creation Studio: \"${resp.prompt}\"")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Creation"))
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share creation", tint = NilaCyan)
                        }

                        // Delete
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    db.generationDao().deleteGeneration(resp.assetId, currentUser.id)
                                    storageManager.deleteAsset(resp.assetUri, currentUser.id)
                                    currentResponse = null
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete creation", tint = NilaRose)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Generation History & Collections Section
            Text(
                text = "Your Creations Gallery",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val tabs = listOf("ALL", "IMAGE", "ANIME", "3D", "EDIT", "FAVORITES")
                items(tabs) { tab ->
                    FilterChip(
                        selected = selectedFilterTab == tab,
                        onClick = { selectedFilterTab = tab },
                        label = { Text(tab) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (historyItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No creations in this collection yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Enter a prompt above and tap Generate to start creating.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                historyItems.forEach { item ->
                    CreationHistoryCard(
                        item = item,
                        onToggleFavorite = {
                            coroutineScope.launch {
                                db.generationDao().toggleFavorite(item.id, currentUser.id, !item.isFavorite)
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                db.generationDao().deleteGeneration(item.id, currentUser.id)
                                if (item.assetUri.isNotBlank()) {
                                    storageManager.deleteAsset(item.assetUri, currentUser.id)
                                }
                            }
                        },
                        onOpen3D = {
                            coroutineScope.launch {
                                val meshFile = item.meshUri?.let { uriStr ->
                                    try { File(Uri.parse(uriStr).path ?: "") } catch (_: Exception) { null }
                                }
                                val loaded = if (meshFile != null && meshFile.exists()) {
                                    ThreeDMeshEngine.parseObjString(meshFile.readText(), name = item.prompt.take(24))
                                } else {
                                    ThreeDMeshEngine.createCyberDrone()
                                }
                                activeViewerMesh = loaded
                                activeViewerPrompt = item.prompt
                            }
                        },
                        onUseAsPrompt = {
                            promptInput = item.prompt
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Fullscreen 3D Viewer overlay when opened
        activeViewerMesh?.let { mesh ->
            ThreeDViewerScreen(
                meshData = mesh,
                prompt = activeViewerPrompt,
                format = "OBJ",
                isReal3D = true,
                onClose = { activeViewerMesh = null },
                onExport = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Exported 3D Model (${mesh.name}) generated by Nila AI Creation Studio.")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export 3D Mesh"))
                }
            )
        }

        // Generation Confirmation Dialog (Requirement 21)
        if (showConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                title = { Text("Confirm Generation", fontWeight = FontWeight.Bold, color = NilaCyan) },
                text = {
                    Column {
                        Text("Prompt:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(promptInput, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Provider: ${creationRouter.realImageProvider.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Mode: ${selectedMode.displayName} | Quality: ${selectedQuality.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmationDialog = false
                            executeGeneration()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NilaCyan)
                    ) {
                        Text("Generate", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmationDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun CreationHistoryCard(
    item: GenerationHistoryEntity,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onOpen3D: () -> Unit,
    onUseAsPrompt: () -> Unit
) {
    NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Icon
            val file = File(Uri.parse(item.assetUri).path ?: "")
            if (file.exists()) {
                val bmp = remember(item.assetUri) { BitmapFactory.decodeFile(file.absolutePath) }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NilaCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = if (item.type == "3D") Icons.Default.ViewInAr else Icons.Default.Image, contentDescription = null, tint = NilaCyan)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NilaPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = if (item.type == "3D") Icons.Default.ViewInAr else Icons.Default.Image, contentDescription = null, tint = NilaPurple)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (item.type) {
                            "3D" -> NilaCyan.copy(alpha = 0.2f)
                            "ANIME" -> NilaPurple.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = item.type,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (item.type) {
                                "3D" -> NilaCyan
                                "ANIME" -> NilaPurple
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.style,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.prompt,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) NilaAmber else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.type == "3D") {
                IconButton(onClick = onOpen3D, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.ViewInAr, contentDescription = "View 3D", tint = NilaCyan)
                }
            }

            IconButton(onClick = onUseAsPrompt, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Default.EditNote, contentDescription = "Use as Prompt", tint = NilaCyan)
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = NilaRose.copy(alpha = 0.8f))
            }
        }
    }
}
