package com.example.ai.vision

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import com.example.core.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Hand tracking landmark representation with normalized coordinates.
 */
data class HandLandmark(
    val x: Float,
    val y: Float,
    val landmarkType: String
)

/**
 * Hand tracking detection result.
 */
data class HandTrackingResult(
    val isHandPresent: Boolean,
    val isRightHand: Boolean?,
    val confidence: Float,
    val wrist: HandLandmark?,
    val palmCenter: HandLandmark?,
    val thumbTip: HandLandmark?,
    val indexTip: HandLandmark?,
    val middleTip: HandLandmark?,
    val ringTip: HandLandmark?,
    val pinkyTip: HandLandmark?,
    val boundingBox: RectF?
)

/**
 * Object detection result.
 */
data class ObjectDetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: Rect?,
    val description: String
)

/**
 * Clean architecture interface for Face Detection.
 */
interface FaceDetectionEngine {
    suspend fun analyzeFrame(imageProxy: ImageProxy): FaceAndEyeResult
    fun close()
}

/**
 * Clean architecture interface for Eye Interaction & Attention Estimation.
 */
interface EyeInteractionEngine {
    fun evaluateEyeAttention(eulerY: Float, leftEyeOpen: Float?, rightEyeOpen: Float?): EyeInteractionState
}

/**
 * Clean architecture interface for Hand Landmark Tracking.
 */
interface HandTrackingEngine {
    fun trackHand(imageProxy: ImageProxy): HandTrackingResult
}

/**
 * Clean architecture interface for Gesture Recognition.
 */
interface GestureEngineInterface {
    fun analyzeFrame(imageProxy: ImageProxy): GestureResult
    fun reset()
}

/**
 * Clean architecture interface for Optical Character Recognition (OCR).
 */
interface OcrEngineInterface {
    suspend fun processFrame(imageProxy: ImageProxy): OcrResult
    suspend fun processBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): OcrResult
    fun close()
}

/**
 * Clean architecture interface for Object Detection.
 */
interface ObjectDetectionEngine {
    suspend fun detectObjects(bitmap: Bitmap): List<ObjectDetectionResult>
}

/**
 * Clean architecture interface for Camera control and lifecycle.
 */
interface CameraEngine {
    val isCameraActive: Boolean
    val currentLensFacing: Int
    fun startCamera()
    fun stopCamera()
    fun switchLens()
    fun capturePhoto(onSuccess: (Bitmap) -> Unit, onError: (Exception) -> Unit)
}

/**
 * Composite vision engine coordinating face, gesture, eye, OCR, and object detection.
 */
interface VisionEngine {
    val faceDetectionEngine: FaceDetectionEngine
    val eyeInteractionEngine: EyeInteractionEngine
    val handTrackingEngine: HandTrackingEngine
    val gestureEngine: GestureEngineInterface
    val ocrEngine: OcrEngineInterface
    val objectDetectionEngine: ObjectDetectionEngine
}
