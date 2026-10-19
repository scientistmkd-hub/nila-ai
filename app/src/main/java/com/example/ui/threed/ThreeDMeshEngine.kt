package com.example.ui.threed

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.*

data class Vertex3D(val x: Float, val y: Float, val z: Float)
data class Face3D(val v1: Int, val v2: Int, val v3: Int, val normal: Vertex3D = Vertex3D(0f, 1f, 0f))

data class MeshData(
    val vertices: List<Vertex3D>,
    val faces: List<Face3D>,
    val name: String = "NeuralMesh",
    val format: String = "obj"
) {
    val vertexCount: Int get() = vertices.size
    val faceCount: Int get() = faces.size
}

class ThreeDMeshEngine {

    companion object {
        fun createFuturisticCrystal(): MeshData {
            val vertices = listOf(
                Vertex3D(0f, 1.4f, 0f),      // 0: top peak
                Vertex3D(0f, -1.4f, 0f),     // 1: bottom peak
                Vertex3D(0.9f, 0f, 0.4f),    // 2: equatorial vertices
                Vertex3D(-0.9f, 0f, 0.4f),   // 3
                Vertex3D(0f, 0f, -1.0f),     // 4
                Vertex3D(0.7f, 0.5f, -0.4f), // 5: upper facets
                Vertex3D(-0.7f, 0.5f, -0.4f),// 6
                Vertex3D(0f, 0.6f, 0.8f)     // 7
            )

            val faces = listOf(
                Face3D(0, 7, 2),
                Face3D(0, 2, 5),
                Face3D(0, 5, 4),
                Face3D(0, 4, 6),
                Face3D(0, 6, 3),
                Face3D(0, 3, 7),
                Face3D(1, 2, 7),
                Face3D(1, 5, 2),
                Face3D(1, 4, 5),
                Face3D(1, 6, 4),
                Face3D(1, 3, 6),
                Face3D(1, 7, 3)
            )

            return MeshData(vertices, faces, name = "Quantum Prism Crystal", format = "obj")
        }

        fun createCyberDrone(): MeshData {
            val vertices = listOf(
                // Core chassis
                Vertex3D(-0.5f, -0.15f, -0.5f), // 0
                Vertex3D(0.5f, -0.15f, -0.5f),  // 1
                Vertex3D(0.5f, -0.15f, 0.5f),   // 2
                Vertex3D(-0.5f, -0.15f, 0.5f),  // 3
                Vertex3D(-0.3f, 0.25f, -0.3f),  // 4
                Vertex3D(0.3f, 0.25f, -0.3f),   // 5
                Vertex3D(0.3f, 0.25f, 0.3f),    // 6
                Vertex3D(-0.3f, 0.25f, 0.3f),   // 7
                // Sensor dome
                Vertex3D(0f, 0.5f, 0f),         // 8
                // Propulsion thrusters
                Vertex3D(1.1f, 0f, 0f),         // 9
                Vertex3D(-1.1f, 0f, 0f),        // 10
                Vertex3D(0f, 0f, 1.1f),         // 11
                Vertex3D(0f, 0f, -1.1f)         // 12
            )

            val faces = listOf(
                // Bottom
                Face3D(0, 1, 2), Face3D(0, 2, 3),
                // Core body
                Face3D(0, 4, 5), Face3D(0, 5, 1),
                Face3D(1, 5, 6), Face3D(1, 6, 2),
                Face3D(2, 6, 7), Face3D(2, 7, 3),
                Face3D(3, 7, 4), Face3D(3, 4, 0),
                // Top cap
                Face3D(4, 8, 5), Face3D(5, 8, 6),
                Face3D(6, 8, 7), Face3D(7, 8, 4),
                // Thruster struts
                Face3D(1, 9, 2), Face3D(0, 10, 3),
                Face3D(2, 11, 3), Face3D(0, 12, 1)
            )

            return MeshData(vertices, faces, name = "Nila Cyber Drone", format = "obj")
        }

        fun createCyberPyramid(): MeshData {
            val vertices = listOf(
                Vertex3D(0f, 1.2f, 0f),      // 0: Apex
                Vertex3D(-1.0f, -0.8f, 1.0f),  // 1: Front-left
                Vertex3D(1.0f, -0.8f, 1.0f),   // 2: Front-right
                Vertex3D(1.0f, -0.8f, -1.0f),  // 3: Back-right
                Vertex3D(-1.0f, -0.8f, -1.0f), // 4: Back-left
                Vertex3D(0f, -0.8f, 0f)        // 5: Center base
            )
            val faces = listOf(
                Face3D(0, 1, 2), // Front
                Face3D(0, 2, 3), // Right
                Face3D(0, 3, 4), // Back
                Face3D(0, 4, 1), // Left
                Face3D(1, 5, 2), // Base triangles
                Face3D(2, 5, 3),
                Face3D(3, 5, 4),
                Face3D(4, 5, 1)
            )
            return MeshData(vertices, faces, name = "Cyber Nexus Pyramid", format = "obj")
        }

        fun parseObjString(objContent: String, name: String = "Uploaded Mesh"): MeshData {
            val vertices = mutableListOf<Vertex3D>()
            val faces = mutableListOf<Face3D>()

            objContent.lineSequence().forEach { line ->
                val tokens = line.trim().split(Regex("\\s+"))
                if (tokens.isEmpty()) return@forEach

                when (tokens[0]) {
                    "v" -> {
                        if (tokens.size >= 4) {
                            val x = tokens[1].toFloatOrNull() ?: 0f
                            val y = tokens[2].toFloatOrNull() ?: 0f
                            val z = tokens[3].toFloatOrNull() ?: 0f
                            vertices.add(Vertex3D(x, y, z))
                        }
                    }
                    "f" -> {
                        if (tokens.size >= 4) {
                            // OBJ is 1-indexed; parse indices (ignoring vertex textures/normals e.g. "1/2/3")
                            val v1 = (tokens[1].split("/")[0].toIntOrNull() ?: 1) - 1
                            val v2 = (tokens[2].split("/")[0].toIntOrNull() ?: 1) - 1
                            val v3 = (tokens[3].split("/")[0].toIntOrNull() ?: 1) - 1
                            if (v1 in vertices.indices && v2 in vertices.indices && v3 in vertices.indices) {
                                faces.add(Face3D(v1, v2, v3))
                            }
                        }
                    }
                }
            }

            return if (vertices.isNotEmpty() && faces.isNotEmpty()) {
                MeshData(vertices, faces, name = name, format = "obj")
            } else {
                createFuturisticCrystal()
            }
        }
    }

    suspend fun generateThumbnail(
        mesh: MeshData,
        width: Int = 400,
        height: Int = 400,
        primaryColor: Int = android.graphics.Color.CYAN
    ): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.parseColor("#080D1A"))

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = primaryColor
        }

        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = android.graphics.Color.argb(80, 0, 229, 255)
        }

        renderMeshToCanvas(
            canvas = canvas,
            mesh = mesh,
            width = width.toFloat(),
            height = height.toFloat(),
            yaw = 35f,
            pitch = 20f,
            zoom = 1.0f,
            wireframe = false,
            fillPaint = fillPaint,
            strokePaint = paint
        )

        bitmap
    }

    fun renderMeshToCanvas(
        canvas: Canvas,
        mesh: MeshData,
        width: Float,
        height: Float,
        yaw: Float,
        pitch: Float,
        zoom: Float,
        wireframe: Boolean,
        fillPaint: Paint,
        strokePaint: Paint
    ) {
        val radYaw = Math.toRadians(yaw.toDouble()).toFloat()
        val radPitch = Math.toRadians(pitch.toDouble()).toFloat()

        val cosY = cos(radYaw)
        val sinY = sin(radYaw)
        val cosP = cos(radPitch)
        val sinP = sin(radPitch)

        val scale = min(width, height) * 0.32f * zoom
        val centerX = width / 2f
        val centerY = height / 2f

        // Transform all vertices
        val transformed = mesh.vertices.map { v ->
            // Rotate around Y (yaw)
            val x1 = v.x * cosY + v.z * sinY
            val y1 = v.y
            val z1 = -v.x * sinY + v.z * cosY

            // Rotate around X (pitch)
            val x2 = x1
            val y2 = y1 * cosP - z1 * sinP
            val z2 = y1 * sinP + z1 * cosP

            // Perspective division
            val distance = 3.5f
            val fov = 1.0f / (distance - z2).coerceAtLeast(0.5f)
            val screenX = centerX + x2 * scale * fov
            val screenY = centerY - y2 * scale * fov // Flip Y for screen coords

            Triple(screenX, screenY, z2)
        }

        // Depth sort faces back to front (Painter's algorithm)
        val sortedFaces = mesh.faces.map { f ->
            val zAvg = (transformed[f.v1].third + transformed[f.v2].third + transformed[f.v3].third) / 3f
            Pair(f, zAvg)
        }.sortedBy { it.second }

        val path = Path()
        sortedFaces.forEach { (face, _) ->
            val p1 = transformed[face.v1]
            val p2 = transformed[face.v2]
            val p3 = transformed[face.v3]

            path.reset()
            path.moveTo(p1.first, p1.second)
            path.lineTo(p2.first, p2.second)
            path.lineTo(p3.first, p3.second)
            path.close()

            if (!wireframe) {
                canvas.drawPath(path, fillPaint)
            }
            canvas.drawPath(path, strokePaint)
        }
    }
}
