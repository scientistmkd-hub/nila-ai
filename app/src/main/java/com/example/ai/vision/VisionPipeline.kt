package com.example.ai.vision

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

data class VisionPipelineResult(
    val visionMode: VisionMode = VisionMode.GESTURES,
    val faceResult: FaceAndEyeResult? = null,
    val gestureResult: GestureResult? = null,
    val ocrResult: OcrResult? = null,
    val objectRecognitionResult: String? = null,
    val fps: Int = 0,
    val isThrottled: Boolean = false
)

class VisionPipeline(
    private var visionMode: VisionMode = VisionMode.GESTURES,
    private var visionQuality: VisionQuality = VisionQuality.MEDIUM
) : ImageAnalysis.Analyzer, VisionEngine {

    override val faceDetectionEngine = FaceAndEyeEngine()
    val faceAndEyeEngine: FaceAndEyeEngine get() = faceDetectionEngine
    override val eyeInteractionEngine = faceDetectionEngine
    override val gestureEngine = GestureEngine()
    override val handTrackingEngine = gestureEngine
    override val ocrEngine = OcrEngine()
    override val objectDetectionEngine = AndroidObjectDetectionEngine()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isProcessing = AtomicBoolean(false)
    private var lastProcessedFrameTimestamp = 0L

    private val _pipelineState = MutableStateFlow(VisionPipelineResult(visionMode = visionMode))
    val pipelineState: StateFlow<VisionPipelineResult> = _pipelineState.asStateFlow()

    fun setVisionMode(mode: VisionMode) {
        this.visionMode = mode
        gestureEngine.reset()
        _pipelineState.value = _pipelineState.value.copy(visionMode = mode)
    }

    fun setVisionQuality(quality: VisionQuality) {
        this.visionQuality = quality
    }

    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        val minIntervalMs = 1000L / visionQuality.targetFps

        // Frame rate throttling to preserve battery and compute
        if (now - lastProcessedFrameTimestamp < minIntervalMs) {
            imageProxy.close()
            return
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        lastProcessedFrameTimestamp = now

        scope.launch {
            try {
                when (visionMode) {
                    VisionMode.ORIGINAL -> {
                        // Original camera feed: zero artificial overlays, maximum battery saving
                        _pipelineState.value = _pipelineState.value.copy(
                            faceResult = null,
                            gestureResult = null,
                            ocrResult = null,
                            objectRecognitionResult = null
                        )
                    }
                    VisionMode.GESTURES -> {
                        val gesture = gestureEngine.analyzeFrame(imageProxy)
                        _pipelineState.value = _pipelineState.value.copy(
                            gestureResult = gesture,
                            faceResult = null,
                            ocrResult = null
                        )
                    }
                    VisionMode.FACE_DETECTION -> {
                        val face = faceAndEyeEngine.analyzeFrame(imageProxy)
                        _pipelineState.value = _pipelineState.value.copy(
                            faceResult = face,
                            gestureResult = null,
                            ocrResult = null
                        )
                    }
                    VisionMode.OCR_TEXT, VisionMode.DOCUMENT_SCAN -> {
                        val ocr = ocrEngine.processFrame(imageProxy)
                        _pipelineState.value = _pipelineState.value.copy(
                            ocrResult = ocr,
                            faceResult = null,
                            gestureResult = null
                        )
                    }
                    VisionMode.OBJECT_DETECTION, VisionMode.STUDY_SCAN, VisionMode.VISION_ASSISTANT -> {
                        _pipelineState.value = _pipelineState.value.copy(
                            objectRecognitionResult = "Target acquired. Tap capture for ML Kit neural analysis.",
                            faceResult = null,
                            gestureResult = null,
                            ocrResult = null
                        )
                    }
                }
            } catch (e: Exception) {
                // Graceful error recovery
            } finally {
                imageProxy.close()
                isProcessing.set(false)
            }
        }
    }

    fun release() {
        scope.cancel()
        faceAndEyeEngine.close()
        ocrEngine.close()
        gestureEngine.reset()
    }
}
