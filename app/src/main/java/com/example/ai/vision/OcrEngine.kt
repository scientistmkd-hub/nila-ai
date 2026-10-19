package com.example.ai.vision

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class OcrResult(
    val fullText: String,
    val lineCount: Int,
    val hasText: Boolean,
    val statusMessage: String
)

class OcrEngine : OcrEngineInterface {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override suspend fun processFrame(imageProxy: ImageProxy): OcrResult {
        val mediaImage = imageProxy.image ?: return OcrResult("", 0, false, "No image")
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        return executeRecognition(inputImage)
    }

    override suspend fun processBitmap(bitmap: Bitmap, rotationDegrees: Int): OcrResult {
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        return executeRecognition(inputImage)
    }

    private suspend fun executeRecognition(inputImage: InputImage): OcrResult {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    val lineCount = visionText.textBlocks.sumOf { it.lines.size }
                    val result = if (text.isNotBlank()) {
                        OcrResult(
                            fullText = text,
                            lineCount = lineCount,
                            hasText = true,
                            statusMessage = "Extracted $lineCount lines of text"
                        )
                    } else {
                        OcrResult(
                            fullText = "",
                            lineCount = 0,
                            hasText = false,
                            statusMessage = "No readable text detected in view"
                        )
                    }
                    if (continuation.isActive) continuation.resume(result)
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(
                            OcrResult(
                                fullText = "",
                                lineCount = 0,
                                hasText = false,
                                statusMessage = "OCR error: ${it.localizedMessage}"
                            )
                        )
                    }
                }
        }
    }

    override fun close() {
        recognizer.close()
    }
}
