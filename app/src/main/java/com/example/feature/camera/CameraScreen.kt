package com.example.feature.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.OrbStateController
import com.example.ai.vision.VisionPipeline
import com.example.core.memory.MemoryRepository
import com.example.core.model.*
import com.example.data.preferences.NilaPreferences
import com.example.data.preferences.NilaUserSettings
import com.example.ui.components.NilaGlassCard
import com.example.ui.components.NilaOrb
import com.example.ui.components.NilaTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    networkStatus: NetworkStatus,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { NilaPreferences(context) }
    val settings by preferences.settingsFlow.collectAsStateWithLifecycle(initialValue = NilaUserSettings())
    val orbController = remember { OrbStateController() }
    val memoryRepo = remember { MemoryRepository(context) }
    val ttsEngine = remember { com.example.ai.voice.AndroidTtsEngine(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionRationale by remember { mutableStateOf(false) }

    var isCameraActive by remember { mutableStateOf(true) }
    var selectedLensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var activeVisionMode by remember { mutableStateOf(VisionMode.ORIGINAL) }

    // Phase 3 Recognition States
    var isPersonRecognitionEnabled by remember { mutableStateOf(true) }
    var activeMatchResult by remember { mutableStateOf<PersonMatchResult?>(null) }
    var showMatchDismissed by remember { mutableStateOf(false) }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var analysisTextResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzingPhoto by remember { mutableStateOf(false) }

    // Dialogs for remembering from photo
    var showRememberPersonDialog by remember { mutableStateOf(false) }
    var newPersonNameInput by remember { mutableStateOf("") }
    var newPersonRelInput by remember { mutableStateOf("Friend") }
    var showRememberObjectDialog by remember { mutableStateOf(false) }
    var newObjectNameInput by remember { mutableStateOf("") }
    var newObjectCatInput by remember { mutableStateOf("Personal Item") }

    val visionPipeline = remember {
        VisionPipeline(visionMode = activeVisionMode)
    }
    val pipelineState by visionPipeline.pipelineState.collectAsStateWithLifecycle()
    val currentOrbState by orbController.currentOrbState.collectAsStateWithLifecycle()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            isCameraActive = true
            showPermissionRationale = false
        } else {
            showPermissionRationale = true
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun capturePhoto() {
        val capture = imageCapture ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val photoFile = File(context.cacheDir, "nila_snap_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    capturedBitmap = bitmap
                    photoFile.delete()
                }

                override fun onError(exception: ImageCaptureException) {
                    analysisTextResult = "Photo capture failed: ${exception.localizedMessage}"
                }
            }
        )
    }

    LaunchedEffect(activeVisionMode) {
        visionPipeline.setVisionMode(activeVisionMode)
    }

    // Connect vision events to central OrbStateController and trigger gesture actions
    LaunchedEffect(pipelineState.gestureResult) {
        val gesture = pipelineState.gestureResult
        if (gesture != null && gesture.gesture != HandGesture.UNKNOWN && gesture.confidence > 0.8f) {
            orbController.onGestureDetected(gesture.gesture)

            when (gesture.gesture) {
                HandGesture.OPEN_PALM -> {
                    // Open Palm interrupts TTS and speech output immediately
                    ttsEngine.stop()
                    orbController.onSpeechInterrupted()
                    analysisTextResult = "Speech stopped via Open Palm gesture"
                }
                HandGesture.V_SIGN -> {
                    if (settings.vSignAction == "Take Photo" && isCameraActive && capturedBitmap == null) {
                        capturePhoto()
                        analysisTextResult = "Photo captured via V-Sign gesture"
                    }
                }
                HandGesture.WAVE -> {
                    if (settings.isTtsEnabled) {
                        ttsEngine.speak(
                            "Hello! I am here.",
                            rate = settings.speechRate,
                            pitch = settings.speechPitch,
                            languageName = settings.voiceLanguage
                        )
                    }
                    analysisTextResult = "Wave acknowledged: \"Hello! I am here.\""
                }
                HandGesture.THUMBS_UP -> {
                    analysisTextResult = "Thumbs Up recognized: Confirmed 👍"
                }
                HandGesture.THUMBS_DOWN -> {
                    analysisTextResult = "Thumbs Down recognized: Cancelled 👎"
                }
                else -> { /* other gestures handled visually */ }
            }
        }
    }

    LaunchedEffect(pipelineState.faceResult?.eyeState) {
        val isLooking = pipelineState.faceResult?.eyeState == EyeInteractionState.LOOKING_TOWARD_DEVICE
        orbController.onEyeAttentionChanged(isLooking)
    }

    // Phase 3 Face Matching against Active User's Private Profiles Only
    LaunchedEffect(pipelineState.faceResult?.primaryFaceInfo, isPersonRecognitionEnabled) {
        val face = pipelineState.faceResult?.primaryFaceInfo
        if (face != null && isPersonRecognitionEnabled && !showMatchDismissed) {
            val match = memoryRepo.matchFaceAgainstSavedProfiles(face, null, 0.82f)
            activeMatchResult = match
            if (match != null && match.isConfidentMatch) {
                orbController.onGestureDetected(HandGesture.WAVE)
            }
        } else if (face == null) {
            activeMatchResult = null
            showMatchDismissed = false
        }
    }

    DisposableEffect(Unit) {
        orbController.setCameraActive(true)
        onDispose {
            orbController.setCameraActive(false)
            ttsEngine.shutdown()
            visionPipeline.release()
            orbController.release()
        }
    }

    fun analyzeCapturedBitmap(actionType: String) {
        val bmp = capturedBitmap ?: return
        isAnalyzingPhoto = true
        analysisTextResult = null

        coroutineScope.launch {
            when (actionType) {
                "OCR" -> {
                    val ocrResult = visionPipeline.ocrEngine.processBitmap(bmp)
                    analysisTextResult = if (ocrResult.hasText) {
                        "OCR Extracted Text:\n\n${ocrResult.fullText}"
                    } else {
                        "No readable text found in this photo."
                    }
                }
                "OBJECTS" -> {
                    val objEngine = com.example.ai.vision.AndroidObjectDetectionEngine()
                    val objects = objEngine.detectObjects(bmp)
                    analysisTextResult = if (objects.isNotEmpty()) {
                        val details = objects.joinToString("\n\n") {
                            "• ${it.label} (${(it.confidence * 100).toInt()}%)\n  ${it.description}"
                        }
                        "ML Kit On-Device Object Detections (${objects.size}):\n\n$details"
                    } else {
                        "No distinct foreground objects detected with confident bounding boxes."
                    }
                }
                "STUDY" -> {
                    val ocrResult = visionPipeline.ocrEngine.processBitmap(bmp)
                    analysisTextResult = if (ocrResult.hasText) {
                        "Study & Problem Analysis:\n\nExtracted Text:\n\"${ocrResult.fullText}\"\n\nNila Guidance:\nIdentify the core question requirements, review relevant textbook formulas, and break into step-by-step subproblems."
                    } else {
                        "Could not read study problem text clearly. Please bring camera closer to the page."
                    }
                }
                "DESCRIBE" -> {
                    analysisTextResult = "Visual snapshot (${bmp.width}x${bmp.height} px). To run detailed multimodal vision reasoning, connect Gemini Cloud AI in Settings."
                }
            }
            isAnalyzingPhoto = false
        }
    }

    Scaffold(
        topBar = {
            NilaTopBar(
                title = "Camera Vision",
                subtitle = if (isCameraActive && capturedBitmap == null) "Perception Live" else "Standby",
                networkStatus = networkStatus,
                orbState = currentOrbState,
                canNavigateBack = true,
                onNavigateBack = {
                    isCameraActive = false
                    onNavigateBack()
                },
                actions = {
                    if (isCameraActive && capturedBitmap == null) {
                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                                imageCapture?.flashMode = flashMode
                            },
                            modifier = Modifier.testTag("camera_flash_toggle")
                        ) {
                            Icon(
                                imageVector = when (flashMode) {
                                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                    else -> Icons.Default.FlashOff
                                },
                                contentDescription = "Toggle flash",
                                tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) NilaAmber else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        IconButton(
                            onClick = {
                                selectedLensFacing = if (selectedLensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier.testTag("switch_camera_lens")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Switch camera lens",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Selector Tabs & Recognition Toggle
            if (capturedBitmap == null) {
                ScrollableTabRow(
                    selectedTabIndex = activeVisionMode.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 8.dp
                ) {
                    VisionMode.values().forEach { mode ->
                        Tab(
                            selected = activeVisionMode == mode,
                            onClick = { activeVisionMode = mode },
                            text = { Text(mode.label, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Phase 3 Recognition Toggle Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPersonRecognitionEnabled) NilaCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isPersonRecognitionEnabled) NilaCyan.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { isPersonRecognitionEnabled = !isPersonRecognitionEnabled }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPersonRecognitionEnabled) Icons.Default.Face else Icons.Default.FaceRetouchingOff,
                                contentDescription = null,
                                tint = if (isPersonRecognitionEnabled) NilaCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersonRecognitionEnabled) "PERSON RECOGNITION: ON" else "PERSON RECOGNITION: OFF",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isPersonRecognitionEnabled) NilaCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Private to Profile",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Viewport Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (capturedBitmap != null) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Captured photo",
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (hasCameraPermission && isCameraActive) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            val executor = ContextCompat.getMainExecutor(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val capture = ImageCapture.Builder()
                                    .setFlashMode(flashMode)
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture

                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(Executors.newSingleThreadExecutor(), visionPipeline)
                                    }

                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(selectedLensFacing)
                                    .build()

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        capture,
                                        analysis
                                    )
                                } catch (_: Exception) {}
                            }, executor)

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top Indicators Overlay: CAMERA ACTIVE & Attention
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NilaEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CAMERA ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        if (pipelineState.faceResult?.eyeState == EyeInteractionState.LOOKING_TOWARD_DEVICE) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = NilaCyan.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Eye Contact",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    // Phase 3 Person Recognition Match Overlay Card
                    if (isPersonRecognitionEnabled && activeMatchResult != null && !showMatchDismissed) {
                        val match = activeMatchResult!!
                        PersonMatchCard(
                            match = match,
                            onConfirm = {
                                analysisTextResult = "Confirmed recognition: ${match.matchedPersonName}"
                                showMatchDismissed = true
                            },
                            onReject = {
                                analysisTextResult = "Marked as not ${match.matchedPersonName}."
                                showMatchDismissed = true
                            },
                            onIgnore = {
                                showMatchDismissed = true
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(14.dp)
                        )
                    }

                    // Gesture Overlay Pill
                    val gesture = pipelineState.gestureResult
                    if (gesture != null && gesture.gesture != HandGesture.UNKNOWN && gesture.confidence > 0.75f && activeMatchResult == null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NilaPurple.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PanTool,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${gesture.gesture.displayName} (${(gesture.confidence * 100).toInt()}%) → ${gesture.gesture.defaultAction}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        NilaOrb(
                            orbState = OrbState.IDLE,
                            size = 120.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (!hasCameraPermission) "Camera Permission Required" else "Optical Perception Suspended",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Post-Capture Toolbar or Live Controls
            if (capturedBitmap != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            capturedBitmap = null
                            analysisTextResult = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retake", maxLines = 1)
                    }

                    Button(
                        onClick = { analyzeCapturedBitmap("OCR") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NilaCyan)
                    ) {
                        Text("OCR", color = Color.Black, maxLines = 1)
                    }

                    Button(
                        onClick = { analyzeCapturedBitmap("OBJECTS") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NilaAmber)
                    ) {
                        Text("Objects", color = Color.Black, maxLines = 1)
                    }

                    Button(
                        onClick = { analyzeCapturedBitmap("STUDY") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NilaPurple)
                    ) {
                        Text("Study", color = Color.White, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { showRememberPersonDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NilaPurple.copy(alpha = 0.8f))
                    ) {
                        Text("+ Person Profile", maxLines = 1)
                    }

                    Button(
                        onClick = { showRememberObjectDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NilaEmerald)
                    ) {
                        Text("+ Save Object", maxLines = 1)
                    }
                }

                if (isAnalyzingPhoto) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing photo...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (analysisTextResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Vision Result",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NilaCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = analysisTextResult ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (hasCameraPermission && isCameraActive) {
                // Live Viewport Shutter Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { isCameraActive = false },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PauseCircle, contentDescription = "Pause camera")
                    }

                    FilledIconButton(
                        onClick = { capturePhoto() },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Camera, contentDescription = "Capture photo", modifier = Modifier.size(36.dp))
                    }

                    IconButton(
                        onClick = {
                            selectedLensFacing = if (selectedLensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FlipCameraAndroid, contentDescription = "Flip camera")
                    }
                }
            } else if (!hasCameraPermission) {
                NilaGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderStroke = BorderStroke(1.dp, NilaRose.copy(alpha = 0.5f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = NilaRose,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Camera permission is required for Nila's vision features.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("Open Settings", maxLines = 1)
                            }
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NilaCyan)
                            ) {
                                Text("Try Again", color = Color.Black, maxLines = 1)
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = { isCameraActive = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NilaCyan)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resume Camera Perception", color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pipeline Telemetry
            NilaGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Vision & Recognition Status",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                val statusText = when {
                    activeMatchResult?.isConfidentMatch == true -> "Person recognized: ${activeMatchResult?.matchedPersonName} (${(activeMatchResult!!.confidence * 100).toInt()}%)"
                    pipelineState.faceResult?.faceCount ?: 0 > 0 -> "Face detected. Recognition active."
                    else -> "Optical perception active. Ready for gestures, faces, and text."
                }
                Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = NilaCyan)
            }
        }
    }

    // Save Person Dialog from Camera Photo
    if (showRememberPersonDialog && capturedBitmap != null) {
        AlertDialog(
            onDismissRequest = { showRememberPersonDialog = false },
            title = { Text("Remember Person from Photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPersonNameInput,
                        onValueChange = { newPersonNameInput = it },
                        label = { Text("Person's Name *") },
                        placeholder = { Text("e.g. Arun") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPersonRelInput,
                        onValueChange = { newPersonRelInput = it },
                        label = { Text("Relationship *") },
                        placeholder = { Text("Project Partner, Friend, Colleague") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPersonNameInput.isNotBlank()) {
                            coroutineScope.launch {
                                memoryRepo.createPersonProfile(
                                    name = newPersonNameInput,
                                    relationship = newPersonRelInput,
                                    photoBitmap = capturedBitmap
                                )
                                showRememberPersonDialog = false
                                newPersonNameInput = ""
                                analysisTextResult = "Profile saved for $newPersonNameInput with local face embedding."
                            }
                        }
                    },
                    enabled = newPersonNameInput.isNotBlank()
                ) {
                    Text("Save to Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRememberPersonDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Save Object Dialog from Camera Photo
    if (showRememberObjectDialog && capturedBitmap != null) {
        AlertDialog(
            onDismissRequest = { showRememberObjectDialog = false },
            title = { Text("Remember Object from Photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newObjectNameInput,
                        onValueChange = { newObjectNameInput = it },
                        label = { Text("Object Name *") },
                        placeholder = { Text("e.g. My Blue Bike") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newObjectCatInput,
                        onValueChange = { newObjectCatInput = it },
                        label = { Text("Category") },
                        placeholder = { Text("Vehicle, Device, Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newObjectNameInput.isNotBlank()) {
                            coroutineScope.launch {
                                memoryRepo.saveObject(
                                    name = newObjectNameInput,
                                    category = newObjectCatInput,
                                    bitmap = capturedBitmap
                                )
                                showRememberObjectDialog = false
                                newObjectNameInput = ""
                                analysisTextResult = "Object saved: $newObjectNameInput with visual features."
                            }
                        }
                    },
                    enabled = newObjectNameInput.isNotBlank()
                ) {
                    Text("Save to Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRememberObjectDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Camera Permission Required") },
            text = { Text("Camera access is needed for live vision analysis, person recognition, and OCR.") },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                        showPermissionRationale = false
                    }
                ) {
                    Text("Open Android Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PersonMatchCard(
    match: PersonMatchResult,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, if (match.isConfidentMatch) NilaCyan else NilaAmber.copy(alpha = 0.6f)),
        tonalElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NilaCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = NilaCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (match.isConfidentMatch) "Possible Match: ${match.matchedPersonName}" else "Low Confidence Match: ${match.matchedPersonName}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${match.relationship} • ${(match.confidence * 100).toInt()}% confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (match.isConfidentMatch) NilaCyan else NilaAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NilaEmerald),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("Confirm", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("Not ${match.matchedPersonName}", maxLines = 1)
                }

                TextButton(
                    onClick = onIgnore,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("Ignore")
                }
            }
        }
    }
}
