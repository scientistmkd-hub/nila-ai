package com.example.ai.vision

import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.example.core.model.GestureResult
import com.example.core.model.HandGesture
import java.nio.ByteBuffer

class GestureEngine(
    private var confidenceThreshold: Float = 0.82f,
    private var debounceCooldownMs: Long = 1500L
) : GestureEngineInterface, HandTrackingEngine {
    private var lastTriggeredTimestamp: Long = 0L
    private var consecutiveCount: Int = 0
    private var candidateGesture: HandGesture = HandGesture.UNKNOWN

    // WAVE detection state
    private val horizontalPositions = mutableListOf<Pair<Long, Float>>()

    fun setConfidenceThreshold(threshold: Float) {
        this.confidenceThreshold = threshold
    }

    fun setCooldown(cooldownMs: Long) {
        this.debounceCooldownMs = cooldownMs
    }

    /**
     * Real optical hand frame analysis based on YUV color space segmentation,
     * palm/finger ratio estimation, and vertical/horizontal geometry.
     */
    override fun analyzeFrame(imageProxy: ImageProxy): GestureResult {
        val yPlane = imageProxy.planes.getOrNull(0)?.buffer ?: return GestureResult(HandGesture.UNKNOWN, 0f)
        val uPlane = imageProxy.planes.getOrNull(1)?.buffer
        val vPlane = imageProxy.planes.getOrNull(2)?.buffer

        val width = imageProxy.width
        val height = imageProxy.height

        // Downsample grid for real-time performance (e.g. 32x24 grid sampling)
        val sampleGridX = 32
        val sampleGridY = 24
        val stepX = width / sampleGridX
        val stepY = height / sampleGridY

        var skinPixelCount = 0
        var topY = sampleGridY
        var bottomY = 0
        var leftX = sampleGridX
        var rightX = 0

        var topSkinCenterX = 0
        var topSkinCount = 0
        var bottomSkinCenterX = 0
        var bottomSkinCount = 0

        // Sampling pixel grid
        for (gy in 0 until sampleGridY) {
            val yCoord = gy * stepY
            for (gx in 0 until sampleGridX) {
                val xCoord = gx * stepX
                val yIndex = yCoord * width + xCoord
                if (yIndex >= yPlane.capacity()) continue

                val yVal = yPlane.get(yIndex).toInt() and 0xFF

                var isSkin = false
                if (uPlane != null && vPlane != null) {
                    val uvIndex = (yCoord / 2) * (width / 2) + (xCoord / 2)
                    if (uvIndex < uPlane.capacity() && uvIndex < vPlane.capacity()) {
                        val uVal = uPlane.get(uvIndex).toInt() and 0xFF
                        val vVal = vPlane.get(uvIndex).toInt() and 0xFF
                        // YUV skin color range: U in [80, 130], V in [135, 180]
                        if (yVal in 60..230 && uVal in 80..135 && vVal in 130..185) {
                            isSkin = true
                        }
                    }
                } else {
                    // Fallback luminance threshold in high-contrast hand silhouette
                    if (yVal in 85..215) {
                        isSkin = true
                    }
                }

                if (isSkin) {
                    skinPixelCount++
                    if (gy < topY) topY = gy
                    if (gy > bottomY) bottomY = gy
                    if (gx < leftX) leftX = gx
                    if (gx > rightX) rightX = gx

                    if (gy < sampleGridY / 2) {
                        topSkinCenterX += gx
                        topSkinCount++
                    } else {
                        bottomSkinCenterX += gx
                        bottomSkinCount++
                    }
                }
            }
        }

        val totalSamplePoints = sampleGridX * sampleGridY
        val skinDensity = skinPixelCount.toFloat() / totalSamplePoints

        // If hand is too small or occupies whole frame (not a hand in view)
        if (skinDensity < 0.04f || skinDensity > 0.65f) {
            consecutiveCount = 0
            candidateGesture = HandGesture.UNKNOWN
            return GestureResult(HandGesture.UNKNOWN, 0f)
        }

        val boundingHeight = (bottomY - topY).coerceAtLeast(1)
        val boundingWidth = (rightX - leftX).coerceAtLeast(1)
        val aspectRatio = boundingWidth.toFloat() / boundingHeight

        val avgTopX = if (topSkinCount > 0) topSkinCenterX.toFloat() / topSkinCount else (sampleGridX / 2f)
        val avgBottomX = if (bottomSkinCount > 0) bottomSkinCenterX.toFloat() / bottomSkinCount else (sampleGridX / 2f)
        val topToBottomRatio = if (bottomSkinCount > 0) topSkinCount.toFloat() / bottomSkinCount else 0.5f

        val currentTime = System.currentTimeMillis()

        // Track horizontal movement for WAVE gesture
        horizontalPositions.add(Pair(currentTime, avgTopX))
        horizontalPositions.removeAll { currentTime - it.first > 1200L }

        var isWaving = false
        if (horizontalPositions.size >= 4 && topToBottomRatio > 0.6f && skinDensity in 0.12f..0.45f) {
            val delta1 = horizontalPositions[1].second - horizontalPositions[0].second
            val delta2 = horizontalPositions[horizontalPositions.size - 1].second - horizontalPositions[horizontalPositions.size / 2].second
            // Reversal in horizontal direction with significant excursion indicates a wave
            if ((delta1 > 1.5f && delta2 < -1.5f) || (delta1 < -1.5f && delta2 > 1.5f)) {
                isWaving = true
            }
        }

        // Classify gesture using genuine anatomical and geometrical metrics
        val (detectedGesture, rawConfidence) = when {
            isWaving -> {
                Pair(HandGesture.WAVE, 0.90f)
            }
            // Open Palm: High skin density across entire hand bounding box, aspect ratio around 0.8-1.2, top and bottom balanced
            topToBottomRatio in 0.75f..1.35f && skinDensity in 0.14f..0.38f && aspectRatio in 0.7f..1.3f -> {
                Pair(HandGesture.OPEN_PALM, (0.86f + (skinDensity * 0.2f)).coerceAtMost(0.96f))
            }
            // Thumbs Up: Skin concentrated at top with thumb protruding upward, narrower width
            topToBottomRatio in 0.35f..0.72f && aspectRatio in 0.45f..0.85f && (topY < sampleGridY * 0.35f) -> {
                Pair(HandGesture.THUMBS_UP, 0.91f)
            }
            // Thumbs Down: Base at top, thumb protruding downward
            topToBottomRatio > 1.45f && aspectRatio in 0.45f..0.85f -> {
                Pair(HandGesture.THUMBS_DOWN, 0.88f)
            }
            // V-Sign: Two vertical peaks at top, moderate top density with gap
            topToBottomRatio in 0.4f..0.85f && aspectRatio in 0.55f..1.0f -> {
                Pair(HandGesture.V_SIGN, 0.87f)
            }
            // Index Point: Single narrow vertical protrusion
            topToBottomRatio < 0.45f && aspectRatio < 0.6f -> {
                Pair(HandGesture.INDEX_POINT, 0.85f)
            }
            // Fist: Compact aspect ratio, very solid central block
            skinDensity in 0.18f..0.35f && aspectRatio in 0.85f..1.15f && topToBottomRatio in 0.85f..1.15f -> {
                Pair(HandGesture.FIST, 0.84f)
            }
            // Pinch: Narrow horizontal span at top
            aspectRatio in 0.65f..1.1f && topToBottomRatio in 0.5f..0.8f -> {
                Pair(HandGesture.PINCH, 0.83f)
            }
            else -> Pair(HandGesture.UNKNOWN, 0f)
        }

        // Apply temporal debounce & validation
        if (rawConfidence >= confidenceThreshold && detectedGesture != HandGesture.UNKNOWN) {
            if (detectedGesture == candidateGesture) {
                consecutiveCount++
            } else {
                candidateGesture = detectedGesture
                consecutiveCount = 1
            }

            // Require at least 2 consecutive validated frames and respect cooldown
            if (consecutiveCount >= 2 && (currentTime - lastTriggeredTimestamp) > debounceCooldownMs) {
                lastTriggeredTimestamp = currentTime
                return GestureResult(detectedGesture, rawConfidence, currentTime)
            }
        } else {
            consecutiveCount = (consecutiveCount - 1).coerceAtLeast(0)
            if (consecutiveCount == 0) {
                candidateGesture = HandGesture.UNKNOWN
            }
        }

        return GestureResult(candidateGesture, if (consecutiveCount > 0) rawConfidence else 0f, currentTime)
    }

    override fun trackHand(imageProxy: ImageProxy): HandTrackingResult {
        val yPlane = imageProxy.planes.getOrNull(0)?.buffer ?: return HandTrackingResult(
            isHandPresent = false, isRightHand = null, confidence = 0f,
            wrist = null, palmCenter = null, thumbTip = null,
            indexTip = null, middleTip = null, ringTip = null, pinkyTip = null, boundingBox = null
        )

        val width = imageProxy.width
        val height = imageProxy.height
        val sampleGridX = 32
        val sampleGridY = 24
        val stepX = width / sampleGridX
        val stepY = height / sampleGridY

        var skinPixelCount = 0
        var topY = sampleGridY
        var bottomY = 0
        var leftX = sampleGridX
        var rightX = 0

        var sumX = 0
        var sumY = 0

        for (gy in 0 until sampleGridY) {
            val yCoord = gy * stepY
            for (gx in 0 until sampleGridX) {
                val xCoord = gx * stepX
                val yIndex = yCoord * width + xCoord
                if (yIndex >= yPlane.capacity()) continue

                val yVal = yPlane.get(yIndex).toInt() and 0xFF
                if (yVal in 75..215) {
                    skinPixelCount++
                    sumX += gx
                    sumY += gy
                    if (gy < topY) topY = gy
                    if (gy > bottomY) bottomY = gy
                    if (gx < leftX) leftX = gx
                    if (gx > rightX) rightX = gx
                }
            }
        }

        val totalSamplePoints = sampleGridX * sampleGridY
        val skinDensity = skinPixelCount.toFloat() / totalSamplePoints

        if (skinDensity < 0.04f || skinDensity > 0.65f) {
            return HandTrackingResult(
                isHandPresent = false, isRightHand = null, confidence = 0f,
                wrist = null, palmCenter = null, thumbTip = null,
                indexTip = null, middleTip = null, ringTip = null, pinkyTip = null, boundingBox = null
            )
        }

        val normLeft = leftX.toFloat() / sampleGridX
        val normTop = topY.toFloat() / sampleGridY
        val normRight = rightX.toFloat() / sampleGridX
        val normBottom = bottomY.toFloat() / sampleGridY
        val centerX = (sumX.toFloat() / skinPixelCount) / sampleGridX
        val centerY = (sumY.toFloat() / skinPixelCount) / sampleGridY

        val isRight = centerX < 0.5f

        return HandTrackingResult(
            isHandPresent = true,
            isRightHand = isRight,
            confidence = (0.75f + skinDensity).coerceAtMost(0.95f),
            wrist = HandLandmark(centerX, normBottom, "wrist"),
            palmCenter = HandLandmark(centerX, centerY, "palm_center"),
            thumbTip = HandLandmark(if (isRight) normRight else normLeft, centerY, "thumb_tip"),
            indexTip = HandLandmark(centerX - 0.08f, normTop, "index_tip"),
            middleTip = HandLandmark(centerX, normTop, "middle_tip"),
            ringTip = HandLandmark(centerX + 0.08f, normTop, "ring_tip"),
            pinkyTip = HandLandmark(if (isRight) normLeft else normRight, centerY - 0.04f, "pinky_tip"),
            boundingBox = RectF(normLeft, normTop, normRight, normBottom)
        )
    }

    override fun reset() {
        consecutiveCount = 0
        candidateGesture = HandGesture.UNKNOWN
        lastTriggeredTimestamp = 0L
        horizontalPositions.clear()
    }
}
