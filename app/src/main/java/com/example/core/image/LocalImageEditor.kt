package com.example.core.image

import android.graphics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

enum class ImageFilterType(val label: String) {
    NONE("Original"),
    CYBERPUNK("Cyber Neon"),
    SEPIA("Sepia Tone"),
    GRAYSCALE("B&W Monochrome"),
    INVERT("Negative"),
    HIGH_CONTRAST("Vivid Dynamic"),
    VIGNETTE("Cinematic Focus")
}

class LocalImageEditor {

    suspend fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap = withContext(Dispatchers.Default) {
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    suspend fun flipBitmap(source: Bitmap, horizontal: Boolean): Bitmap = withContext(Dispatchers.Default) {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    suspend fun cropBitmap(source: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap = withContext(Dispatchers.Default) {
        val clLeft = left.coerceIn(0, source.width - 1)
        val clTop = top.coerceIn(0, source.height - 1)
        val clWidth = width.coerceIn(1, source.width - clLeft)
        val clHeight = height.coerceIn(1, source.height - clTop)
        Bitmap.createBitmap(source, clLeft, clTop, clWidth, clHeight)
    }

    suspend fun adjustBrightnessAndContrast(
        source: Bitmap,
        brightnessOffset: Float, // -100 to 100
        contrastMultiplier: Float // 0.5 to 2.0
    ): Bitmap = withContext(Dispatchers.Default) {
        val output = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // ColorMatrix for contrast and brightness
        val cm = ColorMatrix(
            floatArrayOf(
                contrastMultiplier, 0f, 0f, 0f, brightnessOffset,
                0f, contrastMultiplier, 0f, 0f, brightnessOffset,
                0f, 0f, contrastMultiplier, 0f, brightnessOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        output
    }

    suspend fun applyFilter(source: Bitmap, filter: ImageFilterType): Bitmap = withContext(Dispatchers.Default) {
        if (filter == ImageFilterType.NONE) return@withContext source

        val output = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val cm = when (filter) {
            ImageFilterType.NONE -> ColorMatrix()
            ImageFilterType.GRAYSCALE -> {
                ColorMatrix().apply { setSaturation(0f) }
            }
            ImageFilterType.SEPIA -> {
                val sepiaMatrix = floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                ColorMatrix(sepiaMatrix)
            }
            ImageFilterType.CYBERPUNK -> {
                // Boost blues & cyans, elevate magenta/purple
                val cyberMatrix = floatArrayOf(
                    1.2f, 0.1f, 0.2f, 0f, 20f,
                    0.0f, 1.1f, 0.3f, 0f, 10f,
                    0.2f, 0.2f, 1.5f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                )
                ColorMatrix(cyberMatrix)
            }
            ImageFilterType.INVERT -> {
                val invertMatrix = floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
                ColorMatrix(invertMatrix)
            }
            ImageFilterType.HIGH_CONTRAST -> {
                val contrast = 1.35f
                val t = (1.0f - contrast) / 2.0f * 255.0f
                val hcMatrix = floatArrayOf(
                    contrast, 0f, 0f, 0f, t,
                    0f, contrast, 0f, 0f, t,
                    0f, 0f, contrast, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
                ColorMatrix(hcMatrix)
            }
            ImageFilterType.VIGNETTE -> {
                ColorMatrix()
            }
        }

        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // Draw radial vignette gradient if requested
        if (filter == ImageFilterType.VIGNETTE) {
            val radius = min(source.width, source.height) * 0.8f
            val vignetteGradient = RadialGradient(
                source.width / 2f,
                source.height / 2f,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(160, 0, 0, 0)),
                floatArrayOf(0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val vignettePaint = Paint().apply {
                shader = vignetteGradient
            }
            canvas.drawRect(0f, 0f, source.width.toFloat(), source.height.toFloat(), vignettePaint)
        }

        output
    }
}
