package com.example.core.embedding

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.core.model.DetectedFaceInfo
import org.json.JSONArray
import kotlin.math.sqrt

object EmbeddingEngine {

    /**
     * Computes a normalized 64-dimensional feature vector for a face based on
     * relative landmark bounding, Euler angles, eye open probabilities, and spatial luminance.
     */
    fun computeFaceEmbedding(faceInfo: DetectedFaceInfo, frameBitmap: Bitmap? = null): FloatArray {
        val vector = FloatArray(64)

        // Geometric features (normalized)
        vector[0] = (faceInfo.headEulerY / 45f).coerceIn(-1f, 1f)
        vector[1] = (faceInfo.headEulerZ / 45f).coerceIn(-1f, 1f)
        vector[2] = faceInfo.leftEyeOpenProb ?: 0.5f
        vector[3] = faceInfo.rightEyeOpenProb ?: 0.5f

        val box = faceInfo.boundingBox
        val aspect = box.width().toFloat() / box.height().coerceAtLeast(1)
        vector[4] = (aspect / 2f).coerceIn(0f, 1f)

        // If a bitmap crop is available, compute spatial luminance block averages (7x8 grid = 56 dimensions)
        if (frameBitmap != null && !frameBitmap.isRecycled) {
            val safeLeft = box.left.coerceIn(0, frameBitmap.width - 1)
            val safeTop = box.top.coerceIn(0, frameBitmap.height - 1)
            val safeWidth = box.width().coerceIn(1, frameBitmap.width - safeLeft)
            val safeHeight = box.height().coerceIn(1, frameBitmap.height - safeTop)

            val stepX = safeWidth / 8
            val stepY = safeHeight / 7

            var idx = 5
            for (gy in 0 until 7) {
                for (gx in 0 until 8) {
                    if (idx < 64) {
                        val px = (safeLeft + gx * stepX).coerceIn(0, frameBitmap.width - 1)
                        val py = (safeTop + gy * stepY).coerceIn(0, frameBitmap.height - 1)
                        val color = frameBitmap.getPixel(px, py)
                        val r = (color shr 16) and 0xFF
                        val g = (color shr 8) and 0xFF
                        val b = color and 0xFF
                        val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                        vector[idx++] = lum
                    }
                }
            }
        } else {
            // Populate deterministic normalized harmonic features from bounding properties
            for (i in 5 until 64) {
                vector[i] = ((box.centerX() * (i + 1)) % 100).toFloat() / 100f
            }
        }

        return normalize(vector)
    }

    /**
     * Computes a normalized 64-dimensional visual feature vector for saved objects
     * based on color channels (R, G, B) and spatial texture blocks.
     */
    fun computeObjectEmbedding(bitmap: Bitmap): FloatArray {
        val vector = FloatArray(64)
        if (bitmap.isRecycled) return vector

        val stepX = (bitmap.width / 8).coerceAtLeast(1)
        val stepY = (bitmap.height / 8).coerceAtLeast(1)

        var idx = 0
        for (gy in 0 until 8) {
            for (gx in 0 until 8) {
                if (idx < 64) {
                    val px = (gx * stepX).coerceIn(0, bitmap.width - 1)
                    val py = (gy * stepY).coerceIn(0, bitmap.height - 1)
                    val color = bitmap.getPixel(px, py)
                    val r = ((color shr 16) and 0xFF) / 255f
                    val g = ((color shr 8) and 0xFF) / 255f
                    val b = (color and 0xFF) / 255f
                    vector[idx++] = 0.3f * r + 0.5f * g + 0.2f * b
                }
            }
        }

        return normalize(vector)
    }

    /**
     * Computes a normalized 48-dimensional semantic text feature vector
     * using token n-gram hashing for local offline similarity search.
     */
    fun computeTextEmbedding(text: String): FloatArray {
        val vector = FloatArray(48)
        val words = text.lowercase().split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return vector

        for (word in words) {
            val hash = word.hashCode()
            val bucket = Math.floorMod(hash, 48)
            vector[bucket] += 1.0f

            // Character trigrams
            for (i in 0 until word.length - 2) {
                val triHash = word.substring(i, i + 3).hashCode()
                val triBucket = Math.floorMod(triHash, 48)
                vector[triBucket] += 0.5f
            }
        }

        return normalize(vector)
    }

    /**
     * Standard Cosine Similarity in [-1.0, 1.0], scaled to [0.0, 1.0].
     */
    fun cosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
        if (vecA.isEmpty() || vecB.isEmpty() || vecA.size != vecB.size) return 0f

        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in vecA.indices) {
            dot += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator == 0f) return 0f

        val cos = (dot / denominator).coerceIn(-1f, 1f)
        // Normalize range [-1, 1] to [0, 1]
        return ((cos + 1f) / 2f).coerceIn(0f, 1f)
    }

    private fun normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0f
        for (v in vector) sumSquares += v * v
        val norm = sqrt(sumSquares)
        if (norm > 0f) {
            for (i in vector.indices) vector[i] /= norm
        }
        return vector
    }

    fun vectorToJson(vector: FloatArray): String {
        val json = JSONArray()
        for (v in vector) json.put(v.toDouble())
        return json.toString()
    }

    fun jsonToVector(jsonString: String?): FloatArray? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            val json = JSONArray(jsonString)
            val arr = FloatArray(json.length())
            for (i in 0 until json.length()) {
                arr[i] = json.getDouble(i).toFloat()
            }
            arr
        } catch (_: Exception) {
            null
        }
    }
}
