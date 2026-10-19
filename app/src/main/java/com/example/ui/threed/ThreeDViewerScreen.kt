package com.example.ui.threed

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NilaGlassCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ThreeDViewerScreen(
    meshData: MeshData,
    prompt: String = "",
    format: String = "OBJ",
    isReal3D: Boolean = true,
    onClose: () -> Unit,
    onExport: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var yaw by remember { mutableStateOf(30f) }
    var pitch by remember { mutableStateOf(20f) }
    var zoom by remember { mutableStateOf(1.0f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var isWireframe by remember { mutableStateOf(false) }
    var isAutoRotate by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val meshEngine = remember { ThreeDMeshEngine() }

    // Auto-rotation loop
    LaunchedEffect(isAutoRotate) {
        while (isAutoRotate) {
            delay(25)
            yaw = (yaw + 0.75f) % 360f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
    ) {
        // 3D Canvas Viewport
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("threed_canvas_viewport")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.5f, 3.5f)
                        panX += pan.x
                        panY += pan.y
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        isAutoRotate = false
                        yaw = (yaw + dragAmount.x * 0.6f) % 360f
                        pitch = (pitch + dragAmount.y * 0.6f).coerceIn(-85f, 85f)
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val fillPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = android.graphics.Color.argb(90, 0, 229, 255)
            }

            val strokePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = if (isWireframe) 2.0f else 1.2f
                color = if (isWireframe) android.graphics.Color.CYAN else android.graphics.Color.WHITE
            }

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.translate(panX, panY)

            meshEngine.renderMeshToCanvas(
                canvas = drawContext.canvas.nativeCanvas,
                mesh = meshData,
                width = width,
                height = height,
                yaw = yaw,
                pitch = pitch,
                zoom = zoom,
                wireframe = isWireframe,
                fillPaint = fillPaint,
                strokePaint = strokePaint
            )

            drawContext.canvas.nativeCanvas.restore()
        }

        // Top bar overlay with close and metadata
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("threed_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close 3D Viewer",
                    tint = Color.White
                )
            }

            // Real 3D Badge (Absolute 3D Rule)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isReal3D) NilaCyan.copy(alpha = 0.2f) else NilaAmber.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isReal3D) NilaCyan.copy(alpha = 0.6f) else NilaAmber.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (isReal3D) Icons.Default.ViewInAr else Icons.Default.Image,
                        contentDescription = null,
                        tint = if (isReal3D) NilaCyan else NilaAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isReal3D) "REAL 3D MESH (${format.uppercase()})" else "3D-STYLE PREVIEW (2D)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isReal3D) NilaCyan else NilaAmber
                    )
                }
            }

            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Mesh Information",
                    tint = Color.White
                )
            }
        }

        // Bottom Controls Toolbar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp, start = 20.dp, end = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xDD0D1527),
            border = androidx.compose.foundation.BorderStroke(1.dp, NilaCyan.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Auto-rotate toggle
                IconButton(
                    onClick = { isAutoRotate = !isAutoRotate },
                    modifier = Modifier.testTag("threed_autorotate_toggle")
                ) {
                    Icon(
                        imageVector = if (isAutoRotate) Icons.Default.PlayCircle else Icons.Default.PauseCircle,
                        contentDescription = "Toggle auto-rotate",
                        tint = if (isAutoRotate) NilaCyan else Color.Gray
                    )
                }

                // Wireframe toggle
                IconButton(
                    onClick = { isWireframe = !isWireframe },
                    modifier = Modifier.testTag("threed_wireframe_toggle")
                ) {
                    Icon(
                        imageVector = if (isWireframe) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Toggle wireframe",
                        tint = if (isWireframe) NilaCyan else Color.White
                    )
                }

                // Reset camera
                IconButton(
                    onClick = {
                        yaw = 30f
                        pitch = 20f
                        zoom = 1.0f
                        panX = 0f
                        panY = 0f
                        isAutoRotate = false
                    },
                    modifier = Modifier.testTag("threed_reset_view")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Reset camera position",
                        tint = Color.White
                    )
                }

                // Export / Share
                IconButton(onClick = onExport) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share/Export 3D Model",
                        tint = NilaEmerald
                    )
                }

                // Delete
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete 3D Asset",
                        tint = NilaRose
                    )
                }
            }
        }

        // Info Dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text(meshData.name) },
                text = {
                    Column {
                        Text("Format: ${meshData.format.uppercase()}", style = MaterialTheme.typography.bodyMedium)
                        Text("Vertices: ${meshData.vertexCount}", style = MaterialTheme.typography.bodyMedium)
                        Text("Faces / Triangles: ${meshData.faceCount}", style = MaterialTheme.typography.bodyMedium)
                        if (prompt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Prompt: \"$prompt\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
