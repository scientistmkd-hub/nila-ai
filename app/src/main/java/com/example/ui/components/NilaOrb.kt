package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.model.OrbState
import com.example.core.model.OrbStyle
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Nila AI Official Unique Transparent Futuristic AI Orb Core.
 * Features:
 * - Transparent glass-like outer shell with Fresnel refraction
 * - Glowing internal swirling energy filaments & galaxy core
 * - Dual 3D holographic orbital rings with rotating nodes
 * - Floating quantum energy particles
 * - Crystalline glass specular reflections
 * - Subsurface light halo and floor reflection
 * - Reactive states: IDLE, LISTENING, THINKING, SPEAKING, CAMERA_ACTIVE,
 *   GESTURE_DETECTED, EYE_CONTACT, GENERATING, STUDYING, DOCUMENT_ANALYZING,
 *   CODING, SUCCESS, ERROR, OFFLINE.
 */
@Composable
fun NilaAiOrb(
    orbState: OrbState,
    modifier: Modifier = Modifier,
    orbStyle: OrbStyle = OrbStyle.ENERGY,
    orbDiameter: Dp = 230.dp,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NilaAiOrbTransition")

    // Vertical floating hover animation
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatingHover"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (orbState) {
                    OrbState.LISTENING -> 750
                    OrbState.SPEAKING -> 500
                    OrbState.THINKING -> 600
                    OrbState.GENERATING -> 450
                    OrbState.DOCUMENT_ANALYZING -> 700
                    OrbState.CODING -> 800
                    OrbState.OFFLINE -> 3200
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingPulse"
    )

    // Orbital ring 1 rotation
    val ringRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (orbState) {
                    OrbState.THINKING -> 2000
                    OrbState.GENERATING -> 1600
                    OrbState.CODING -> 2400
                    OrbState.LISTENING -> 3500
                    OrbState.CAMERA_ACTIVE -> 2800
                    OrbState.DOCUMENT_ANALYZING -> 2200
                    else -> 7500
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation1"
    )

    // Orbital ring 2 counter-rotation
    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (orbState) {
                    OrbState.THINKING -> 2600
                    OrbState.GENERATING -> 2200
                    OrbState.CODING -> 3200
                    else -> 9000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation2"
    )

    // Internal energy swirl rotation
    val energySwirlAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EnergySwirl"
    )

    // Document / Camera scanning laser sweep
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanSweep"
    )

    // Audio speaking wave ripple
    val speechWave by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SpeechWave"
    )

    val primaryColor = remember(orbState, orbStyle) { getPrimaryOrbColor(orbState, orbStyle) }
    val secondaryColor = remember(orbState, orbStyle) { getSecondaryOrbColor(orbState, orbStyle) }
    val accentCoreColor = remember(orbState, orbStyle) { getAccentOrbColor(orbState, orbStyle) }

    Box(
        modifier = modifier
            .size(orbDiameter)
            .testTag("nila_orb")
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .pointerInput(Unit) {
                var totalDragY = 0f
                detectDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDragEnd = {
                        if (totalDragY < -40f) {
                            onSwipeUp()
                        } else if (totalDragY > 40f) {
                            onSwipeDown()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragY += dragAmount.y
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f + floatingOffset)
            val outerRadius = size.minDimension / 2.7f
            val dynamicRadius = if (orbState == OrbState.SPEAKING) {
                outerRadius * breathingPulse * speechWave
            } else {
                outerRadius * breathingPulse
            }

            // 1. Underneath Soft Reflection Halo on "Floor"
            val floorCenter = Offset(center.x, size.height * 0.90f)
            val floorWidth = dynamicRadius * 1.8f
            val floorHeight = dynamicRadius * 0.35f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (orbState == OrbState.OFFLINE) 0.08f else 0.32f),
                        secondaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = floorCenter,
                    radius = floorWidth / 2f
                ),
                topLeft = Offset(floorCenter.x - floorWidth / 2f, floorCenter.y - floorHeight / 2f),
                size = Size(floorWidth, floorHeight)
            )

            // 2. Volumetric Ambient Outer Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (orbState == OrbState.OFFLINE) 0.15f else 0.40f),
                        secondaryColor.copy(alpha = 0.18f),
                        accentCoreColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.55f
                ),
                radius = dynamicRadius * 1.55f,
                center = center
            )

            // 3. Holographic Orbital Ring 1 (Tilted 3D Perspective)
            rotate(degrees = ringRotation1, pivot = center) {
                val ringWidth = dynamicRadius * 2.35f
                val ringHeight = dynamicRadius * 0.85f
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.85f),
                            secondaryColor.copy(alpha = 0.25f),
                            accentCoreColor.copy(alpha = 0.90f),
                            primaryColor.copy(alpha = 0.85f)
                        ),
                        center = center
                    ),
                    topLeft = Offset(center.x - ringWidth / 2f, center.y - ringHeight / 2f),
                    size = Size(ringWidth, ringHeight),
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f), ringRotation1 * 0.5f)
                    )
                )

                // Orbiting Quantum Node 1
                val rad1 = Math.toRadians(ringRotation1.toDouble())
                val node1X = center.x + (ringWidth / 2f * cos(rad1)).toFloat()
                val node1Y = center.y + (ringHeight / 2f * sin(rad1)).toFloat()
                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = Offset(node1X, node1Y))
                drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(node1X, node1Y), style = Stroke(1.5.dp.toPx()))
            }

            // 4. Holographic Orbital Ring 2 (Cross Tilt)
            rotate(degrees = ringRotation2 + 55f, pivot = center) {
                val ringWidth2 = dynamicRadius * 1.25f
                val ringHeight2 = dynamicRadius * 2.20f
                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.75f),
                            accentCoreColor.copy(alpha = 0.15f),
                            primaryColor.copy(alpha = 0.80f),
                            secondaryColor.copy(alpha = 0.75f)
                        ),
                        center = center
                    ),
                    topLeft = Offset(center.x - ringWidth2 / 2f, center.y - ringHeight2 / 2f),
                    size = Size(ringWidth2, ringHeight2),
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), ringRotation2 * 0.4f)
                    )
                )

                // Orbiting Quantum Node 2
                val rad2 = Math.toRadians((ringRotation2 + 180).toDouble())
                val node2X = center.x + (ringWidth2 / 2f * cos(rad2)).toFloat()
                val node2Y = center.y + (ringHeight2 / 2f * sin(rad2)).toFloat()
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(node2X, node2Y))
            }

            // 5. Transparent Glass-Like Outer Shell (Subtle Dark Crystalline Vitreous Layer)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x22050B18), // High transparency inner volume
                        Color(0x44081329),
                        primaryColor.copy(alpha = 0.28f),
                        secondaryColor.copy(alpha = 0.45f)
                    ),
                    center = Offset(center.x - dynamicRadius * 0.15f, center.y - dynamicRadius * 0.15f),
                    radius = dynamicRadius
                ),
                radius = dynamicRadius,
                center = center
            )

            // 6. Internal Swirling Galaxy & Energy Core
            rotate(degrees = energySwirlAngle, pivot = center) {
                // Swirling energy filaments
                val filamentCount = 4
                for (i in 0 until filamentCount) {
                    val angleOffset = i * (360f / filamentCount)
                    val rad = Math.toRadians((energySwirlAngle + angleOffset).toDouble())
                    val fx1 = center.x + (dynamicRadius * 0.60f * cos(rad)).toFloat()
                    val fy1 = center.y + (dynamicRadius * 0.60f * sin(rad)).toFloat()

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentCoreColor.copy(alpha = 0.75f),
                                primaryColor.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(fx1, fy1),
                            radius = dynamicRadius * 0.35f
                        ),
                        radius = dynamicRadius * 0.35f,
                        center = Offset(fx1, fy1)
                    )
                }
            }

            // 7. Internal Radiant Energy Singularity Core
            val coreCenter = Offset(center.x - dynamicRadius * 0.08f, center.y - dynamicRadius * 0.08f)
            val coreSize = dynamicRadius * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        accentCoreColor,
                        primaryColor,
                        secondaryColor.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = coreCenter,
                    radius = coreSize
                ),
                radius = coreSize,
                center = coreCenter
            )

            // 8. Floating Micro-Particles inside Glass Sphere
            val particleCount = 12
            for (p in 0 until particleCount) {
                val pAngle = Math.toRadians((energySwirlAngle * (0.8f + p * 0.15f) + p * 30).toDouble())
                val pDist = dynamicRadius * (0.25f + (p % 4) * 0.16f)
                val px = center.x + (pDist * cos(pAngle)).toFloat()
                val py = center.y + (pDist * sin(pAngle)).toFloat()

                drawCircle(
                    color = if (p % 2 == 0) Color.White.copy(alpha = 0.85f) else accentCoreColor.copy(alpha = 0.80f),
                    radius = (1.5f + (p % 3) * 0.8f).dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // 9. Document Analyzing / Camera Scanning Laser Horizon
            if (orbState == OrbState.DOCUMENT_ANALYZING || orbState == OrbState.CAMERA_ACTIVE) {
                val scanY = center.y + (scanProgress * dynamicRadius * 0.75f)
                val scanWidth = dynamicRadius * 1.5f * sqrt(1.0f - (scanProgress * 0.65f).pow(2)).coerceAtLeast(0.3f)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (orbState == OrbState.DOCUMENT_ANALYZING) NilaCyan else NilaPink,
                            Color.White,
                            if (orbState == OrbState.DOCUMENT_ANALYZING) NilaCyan else NilaPink,
                            Color.Transparent
                        ),
                        startX = center.x - scanWidth / 2f,
                        endX = center.x + scanWidth / 2f
                    ),
                    start = Offset(center.x - scanWidth / 2f, scanY),
                    end = Offset(center.x + scanWidth / 2f, scanY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            // 10. Coding Mode Structured Geometric Lattice
            if (orbState == OrbState.CODING) {
                val boxHalf = dynamicRadius * 0.42f
                drawRect(
                    color = NilaEmerald.copy(alpha = 0.65f),
                    topLeft = Offset(center.x - boxHalf, center.y - boxHalf),
                    size = Size(boxHalf * 2f, boxHalf * 2f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                rotate(degrees = 45f, pivot = center) {
                    drawRect(
                        color = NilaCyan.copy(alpha = 0.65f),
                        topLeft = Offset(center.x - boxHalf, center.y - boxHalf),
                        size = Size(boxHalf * 2f, boxHalf * 2f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // 11. Eye Contact Focused Core Iris
            if (orbState == OrbState.EYE_CONTACT) {
                drawCircle(
                    color = Color.White,
                    radius = dynamicRadius * 0.16f,
                    center = center
                )
                drawCircle(
                    color = NilaCyan,
                    radius = dynamicRadius * 0.22f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 12. Transparent Glass Fresnel Rim Reflection (Thin Luminous Glass Edge)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.90f),
                        primaryColor.copy(alpha = 0.70f),
                        Color.Transparent,
                        secondaryColor.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.85f)
                    ),
                    center = center
                ),
                radius = dynamicRadius,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // 13. Crystalline Optical Highlight / Top-Left Glass Specular Crescent
            val highlightRadiusX = dynamicRadius * 0.60f
            val highlightRadiusY = dynamicRadius * 0.28f
            val highlightCenter = Offset(center.x - dynamicRadius * 0.35f, center.y - dynamicRadius * 0.42f)

            rotate(degrees = -30f, pivot = highlightCenter) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.88f),
                            Color.White.copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        center = highlightCenter,
                        radius = highlightRadiusX
                    ),
                    topLeft = Offset(highlightCenter.x - highlightRadiusX, highlightCenter.y - highlightRadiusY),
                    size = Size(highlightRadiusX * 2f, highlightRadiusY * 2f)
                )
            }
        }
    }
}

/**
 * Backward-compatible delegating composable for existing call sites.
 */
@Composable
fun NilaOrb(
    orbState: OrbState,
    modifier: Modifier = Modifier,
    orbStyle: OrbStyle = OrbStyle.ENERGY,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    NilaAiOrb(
        orbState = orbState,
        modifier = modifier,
        orbStyle = orbStyle,
        orbDiameter = size,
        onClick = onClick
    )
}

private fun getPrimaryOrbColor(state: OrbState, style: OrbStyle): Color {
    return when (state) {
        OrbState.OFFLINE -> Color(0xFF64748B)
        OrbState.ERROR -> Color(0xFFEF4444)
        OrbState.LISTENING -> Color(0xFF00E5FF)
        OrbState.THINKING -> Color(0xFFA855F7)
        OrbState.SPEAKING -> Color(0xFF38BDF8)
        OrbState.GENERATING -> Color(0xFFEC4899)
        OrbState.STUDYING -> Color(0xFF6366F1) // Intelligent royal violet/blue
        OrbState.DOCUMENT_ANALYZING -> Color(0xFF06B6D4) // Document laser cyan
        OrbState.CODING -> Color(0xFF10B981) // Matrix emerald
        OrbState.CONTROL_ACTIVE -> Color(0xFF00E5FF) // Cyber electric cyan
        OrbState.TIMER_ACTIVE -> Color(0xFFF59E0B) // Pulsing gold amber
        OrbState.AUTOMATION_ACTIVE -> Color(0xFFA855F7) // Automation quantum violet
        OrbState.CAMERA_ACTIVE -> Color(0xFFF43F5E)
        OrbState.GESTURE_DETECTED -> Color(0xFF10B981)
        OrbState.EYE_CONTACT -> Color(0xFF00E5FF)
        OrbState.SUCCESS -> Color(0xFF10B981)
        OrbState.IDLE -> when (style) {
            OrbStyle.ENERGY -> Color(0xFF00E5FF)
            OrbStyle.GALAXY -> Color(0xFFA855F7)
            OrbStyle.LIQUID -> Color(0xFF06B6D4)
            OrbStyle.CRYSTAL -> Color(0xFFF43F5E)
            OrbStyle.NEON -> Color(0xFF00FF66)
            OrbStyle.MINIMAL -> Color(0xFFE2E8F0)
        }
    }
}

private fun getSecondaryOrbColor(state: OrbState, style: OrbStyle): Color {
    return when (state) {
        OrbState.OFFLINE -> Color(0xFF334155)
        OrbState.ERROR -> Color(0xFF7F1D1D)
        OrbState.LISTENING -> Color(0xFF3B82F6)
        OrbState.THINKING -> Color(0xFF6366F1)
        OrbState.SPEAKING -> Color(0xFFA855F7)
        OrbState.GENERATING -> Color(0xFF8B5CF6)
        OrbState.STUDYING -> Color(0xFF3B82F6)
        OrbState.DOCUMENT_ANALYZING -> Color(0xFF6366F1)
        OrbState.CODING -> Color(0xFF00E5FF)
        OrbState.CONTROL_ACTIVE -> Color(0xFF38BDF8)
        OrbState.TIMER_ACTIVE -> Color(0xFFEF4444)
        OrbState.AUTOMATION_ACTIVE -> Color(0xFFEC4899)
        OrbState.CAMERA_ACTIVE -> Color(0xFFA855F7)
        OrbState.GESTURE_DETECTED -> Color(0xFF00E5FF)
        OrbState.EYE_CONTACT -> Color(0xFF8B5CF6)
        OrbState.SUCCESS -> Color(0xFF00E5FF)
        OrbState.IDLE -> when (style) {
            OrbStyle.ENERGY -> Color(0xFF8B5CF6)
            OrbStyle.GALAXY -> Color(0xFFEC4899)
            OrbStyle.LIQUID -> Color(0xFF3B82F6)
            OrbStyle.CRYSTAL -> Color(0xFF8B5CF6)
            OrbStyle.NEON -> Color(0xFF00E5FF)
            OrbStyle.MINIMAL -> Color(0xFF94A3B8)
        }
    }
}

private fun getAccentOrbColor(state: OrbState, style: OrbStyle): Color {
    return when (state) {
        OrbState.OFFLINE -> Color(0xFF94A3B8)
        OrbState.ERROR -> Color(0xFFFCA5A5)
        OrbState.LISTENING -> Color(0xFFE0F2FE)
        OrbState.THINKING -> Color(0xFFF3E8FF)
        OrbState.SPEAKING -> Color(0xFFFFFFFF)
        OrbState.GENERATING -> Color(0xFFFCE7F3)
        OrbState.STUDYING -> Color(0xFFC7D2FE)
        OrbState.DOCUMENT_ANALYZING -> Color(0xFFCFFAFE)
        OrbState.CODING -> Color(0xFFA7F3D0)
        OrbState.CONTROL_ACTIVE -> Color(0xFFE0F2FE)
        OrbState.TIMER_ACTIVE -> Color(0xFFFEF3C7)
        OrbState.AUTOMATION_ACTIVE -> Color(0xFFF3E8FF)
        OrbState.CAMERA_ACTIVE -> Color(0xFFFFE4E6)
        OrbState.GESTURE_DETECTED -> Color(0xFFA7F3D0)
        OrbState.EYE_CONTACT -> Color(0xFFE0F2FE)
        OrbState.SUCCESS -> Color(0xFFFFFFFF)
        OrbState.IDLE -> Color(0xFFFFFFFF)
    }
}
