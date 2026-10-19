package com.example.ai.vision

import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.example.core.model.DetectedFaceInfo
import com.example.core.model.EyeInteractionState
import com.example.core.model.FaceState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class FaceAndEyeResult(
    val faceState: FaceState,
    val eyeState: EyeInteractionState,
    val faceCount: Int,
    val primaryFaceInfo: DetectedFaceInfo?,
    val statusMessage: String
)

class FaceAndEyeEngine : FaceDetectionEngine, EyeInteractionEngine {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .enableTracking()
        .build()

    private val detector by lazy { FaceDetection.getClient(options) }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override suspend fun analyzeFrame(imageProxy: ImageProxy): FaceAndEyeResult {
        val mediaImage = imageProxy.image ?: return FaceAndEyeResult(
            faceState = FaceState.NO_FACE,
            eyeState = EyeInteractionState.NO_FACE,
            faceCount = 0,
            primaryFaceInfo = null,
            statusMessage = "No camera frame"
        )

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        return suspendCancellableCoroutine { continuation ->
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    val result = processDetectedFaces(faces)
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(
                            FaceAndEyeResult(
                                faceState = FaceState.LOW_CONFIDENCE,
                                eyeState = EyeInteractionState.LOW_CONFIDENCE,
                                faceCount = 0,
                                primaryFaceInfo = null,
                                statusMessage = "Face detection error: ${it.localizedMessage}"
                            )
                        )
                    }
                }
        }
    }

    private fun processDetectedFaces(faces: List<Face>): FaceAndEyeResult {
        if (faces.isEmpty()) {
            return FaceAndEyeResult(
                faceState = FaceState.NO_FACE,
                eyeState = EyeInteractionState.NO_FACE,
                faceCount = 0,
                primaryFaceInfo = null,
                statusMessage = "No face in view"
            )
        }

        if (faces.size > 1) {
            val primary = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            val eyeState = evaluateEyeAttention(primary)
            return FaceAndEyeResult(
                faceState = FaceState.MULTIPLE_FACES,
                eyeState = eyeState,
                faceCount = faces.size,
                primaryFaceInfo = primary?.toDetectedFaceInfo(eyeState == EyeInteractionState.LOOKING_TOWARD_DEVICE),
                statusMessage = "${faces.size} people detected. Focus on primary subject."
            )
        }

        val face = faces[0]
        val eyeState = evaluateEyeAttention(face)
        val faceState = if (face.trackingId != null) FaceState.TRACKING else FaceState.FACE_DETECTED

        val statusMsg = when (eyeState) {
            EyeInteractionState.LOOKING_TOWARD_DEVICE -> "Device-facing attention detected"
            EyeInteractionState.LOOKING_AWAY -> "Subject looking away"
            else -> "Face tracking active"
        }

        return FaceAndEyeResult(
            faceState = faceState,
            eyeState = eyeState,
            faceCount = 1,
            primaryFaceInfo = face.toDetectedFaceInfo(eyeState == EyeInteractionState.LOOKING_TOWARD_DEVICE),
            statusMessage = statusMsg
        )
    }

    fun evaluateEyeAttention(face: Face?): EyeInteractionState {
        if (face == null) return EyeInteractionState.NO_FACE
        return calculateEyeAttention(face.headEulerAngleY, face.leftEyeOpenProbability, face.rightEyeOpenProbability)
    }

    private fun Face.toDetectedFaceInfo(isLookingAtDevice: Boolean): DetectedFaceInfo {
        return DetectedFaceInfo(
            boundingBox = this.boundingBox,
            trackingId = this.trackingId,
            headEulerY = this.headEulerAngleY,
            headEulerZ = this.headEulerAngleZ,
            leftEyeOpenProb = this.leftEyeOpenProbability,
            rightEyeOpenProb = this.rightEyeOpenProbability,
            isLookingAtDevice = isLookingAtDevice
        )
    }

    override fun close() {
        try {
            detector.close()
        } catch (_: Exception) {}
    }

    override fun evaluateEyeAttention(eulerY: Float, leftEyeOpen: Float?, rightEyeOpen: Float?): EyeInteractionState {
        return calculateEyeAttention(eulerY, leftEyeOpen, rightEyeOpen)
    }

    companion object {
        fun calculateEyeAttention(eulerY: Float, leftEyeOpen: Float?, rightEyeOpen: Float?): EyeInteractionState {
            if (leftEyeOpen != null && rightEyeOpen != null && leftEyeOpen < 0.25f && rightEyeOpen < 0.25f) {
                return EyeInteractionState.LOOKING_AWAY
            }
            return if (eulerY in -16.0f..16.0f) {
                EyeInteractionState.LOOKING_TOWARD_DEVICE
            } else {
                EyeInteractionState.LOOKING_AWAY
            }
        }
    }
}
