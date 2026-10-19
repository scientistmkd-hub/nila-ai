package com.example.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class StorageStatistics(
    val totalCreationsBytes: Long,
    val totalCacheBytes: Long,
    val availableDeviceBytes: Long,
    val totalCreationsCount: Int
)

class CreationStorageManager(private val context: Context) {

    private val creationsDir: File by lazy {
        File(context.filesDir, "nila_creations").apply { if (!exists()) mkdirs() }
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, "nila_gen_cache").apply { if (!exists()) mkdirs() }
    }

    suspend fun saveGeneratedBitmap(
        bitmap: Bitmap,
        userId: String,
        prefix: String = "nila_img",
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): File = withContext(Dispatchers.IO) {
        val userDir = File(creationsDir, sanitizeUserId(userId)).apply { if (!exists()) mkdirs() }
        val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val file = File(userDir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension")
        FileOutputStream(file).use { out ->
            bitmap.compress(format, quality, out)
        }
        file
    }

    suspend fun save3DAsset(
        bytes: ByteArray,
        userId: String,
        extension: String = "glb",
        prefix: String = "nila_3d"
    ): File = withContext(Dispatchers.IO) {
        val userDir = File(creationsDir, sanitizeUserId(userId)).apply { if (!exists()) mkdirs() }
        val file = File(userDir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$extension")
        FileOutputStream(file).use { out ->
            out.write(bytes)
        }
        file
    }

    suspend fun createCacheFile(prefix: String, extension: String): File = withContext(Dispatchers.IO) {
        File(cacheDir, "${prefix}_${System.currentTimeMillis()}.$extension")
    }

    suspend fun getStorageStatistics(userId: String): StorageStatistics = withContext(Dispatchers.IO) {
        val userDir = File(creationsDir, sanitizeUserId(userId))
        var creationsBytes = 0L
        var creationsCount = 0

        if (userDir.exists()) {
            userDir.listFiles()?.forEach { f ->
                if (f.isFile) {
                    creationsBytes += f.length()
                    creationsCount++
                }
            }
        }

        var cacheBytes = 0L
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { f ->
                if (f.isFile) {
                    cacheBytes += f.length()
                }
            }
        }

        val freeSpace = context.filesDir.usableSpace

        StorageStatistics(
            totalCreationsBytes = creationsBytes,
            totalCacheBytes = cacheBytes,
            availableDeviceBytes = freeSpace,
            totalCreationsCount = creationsCount
        )
    }

    suspend fun clearGenerationCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteAsset(fileUriString: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(fileUriString)
            val file = if (uri.scheme == "file") File(uri.path ?: "") else File(fileUriString)
            // Ensure security check: file must reside inside this user's directory
            val expectedParent = File(creationsDir, sanitizeUserId(userId)).canonicalPath
            if (file.exists() && file.canonicalPath.startsWith(expectedParent)) {
                file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun wipeAllUserData(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userDir = File(creationsDir, sanitizeUserId(userId))
            if (userDir.exists()) {
                userDir.deleteRecursively()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun validateImageFile(file: File): Boolean {
        if (!file.exists() || file.length() <= 0) return false
        if (file.length() > 25 * 1024 * 1024) return false // reject files > 25MB
        val ext = file.extension.lowercase()
        if (ext !in listOf("png", "jpg", "jpeg", "webp")) return false

        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth > 0 && options.outHeight > 0
        } catch (_: Exception) {
            false
        }
    }

    fun validate3DFile(file: File): Boolean {
        if (!file.exists() || file.length() <= 0) return false
        if (file.length() > 50 * 1024 * 1024) return false // reject meshes > 50MB
        val ext = file.extension.lowercase()
        return ext in listOf("glb", "gltf", "obj")
    }

    private fun sanitizeUserId(userId: String): String {
        return userId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}
