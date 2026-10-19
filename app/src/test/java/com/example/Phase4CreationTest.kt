package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.creation.CreationRouter
import com.example.core.image.ImageFilterType
import com.example.core.image.LocalImageEditor
import com.example.core.model.*
import com.example.core.prompt.PromptProcessor
import com.example.core.prompt.PromptValidationResult
import com.example.core.storage.CreationStorageManager
import com.example.data.local.NilaDatabase
import com.example.data.local.entity.GenerationHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase4CreationTest {

    private lateinit var context: Context
    private lateinit var db: NilaDatabase
    private lateinit var promptProcessor: PromptProcessor
    private lateinit var storageManager: CreationStorageManager
    private lateinit var creationRouter: CreationRouter
    private lateinit var localImageEditor: LocalImageEditor

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NilaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        promptProcessor = PromptProcessor()
        storageManager = CreationStorageManager(context)
        creationRouter = CreationRouter(context, storageManager)
        localImageEditor = LocalImageEditor()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testPromptValidation() {
        // Empty prompt should fail
        val emptyResult = promptProcessor.validatePrompt("")
        assertTrue(emptyResult is PromptValidationResult.Invalid)

        // Too brief prompt should fail
        val briefResult = promptProcessor.validatePrompt("hi")
        assertTrue(briefResult is PromptValidationResult.Invalid)

        // Valid prompt should pass
        val validResult = promptProcessor.validatePrompt("Futuristic city of Chennai at sunset")
        assertTrue(validResult is PromptValidationResult.Valid)
    }

    @Test
    fun testPromptEnhancementPreservesIntent() {
        val request = CreationRequest(
            prompt = "Floating space station",
            style = ImageStyle.SCI_FI,
            mode = CreationMode.IMAGE
        )
        val processed = promptProcessor.process(request, enableEnhancement = true)
        assertTrue(processed.enhancedPrompt.contains("Floating space station"))
        assertTrue(processed.enhancedPrompt.contains("futuristic science fiction"))
    }

    @Test
    fun testFileValidation() {
        val testFile = File(context.cacheDir, "test_file.obj")
        testFile.writeText("v 0 0 0\nv 1 1 1\nv 0 1 0\nf 1 2 3")
        assertTrue(storageManager.validate3DFile(testFile))

        val invalidExtFile = File(context.cacheDir, "test_file.exe")
        invalidExtFile.writeText("untrusted binary content")
        assertFalse(storageManager.validate3DFile(invalidExtFile))
        assertFalse(storageManager.validateImageFile(invalidExtFile))

        testFile.delete()
        invalidExtFile.delete()
    }

    @Test
    fun testLocalImageGenerationRouting() = runBlocking {
        val request = CreationRequest(
            prompt = "Quantum energy orb over neon ocean",
            style = ImageStyle.SCI_FI,
            aspectRatio = CreationAspectRatio.RATIO_1_1,
            mode = CreationMode.IMAGE
        )
        val result = creationRouter.routeImageGeneration(request, userId = "user_test_1")
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertNotNull(response.assetUri)
        assertEquals(AssetType.IMAGE_2D, response.assetType)
        assertFalse(response.isReal3D)
    }

    @Test
    fun testRealThreeDGenerationRouting() = runBlocking {
        val request = CreationRequest(
            prompt = "Cyber drone with quad propulsion",
            mode = CreationMode.THREE_D
        )
        val result = creationRouter.route3DGeneration(request, userId = "user_test_1")
        assertTrue(result.isSuccess)
        val response = result.getOrThrow()

        // Verify Absolute 3D Rule
        assertTrue(response.isReal3D)
        assertEquals(AssetType.REAL_3D_MODEL, response.assetType)
        assertEquals(ThreeDFormat.OBJ, response.threeDFormat)
        assertNotNull(response.meshUri)
        assertNotNull(response.thumbnailUri)
    }

    @Test
    fun testCriticalUserIsolation() = runBlocking {
        // User A creates an asset
        val userAId = "user_alice_456"
        val userBId = "user_bob_789"

        val entityA = GenerationHistoryEntity(
            id = UUID.randomUUID().toString(),
            userId = userAId,
            prompt = "Alice's private architectural concept",
            type = "IMAGE",
            style = "Architecture",
            provider = "Local Engine",
            status = "SUCCESS",
            assetUri = "file:///data/user/0/creations/user_alice/img.png"
        )
        db.generationDao().insertGeneration(entityA)

        // Verify User A can see their own creation
        val userAGenerations = db.generationDao().getGenerationsForUser(userAId).first()
        assertEquals(1, userAGenerations.size)
        assertEquals("Alice's private architectural concept", userAGenerations[0].prompt)

        // CRITICAL CHECK: User B queries their generations -> User A's asset must NOT appear!
        val userBGenerations = db.generationDao().getGenerationsForUser(userBId).first()
        assertEquals(0, userBGenerations.size)

        // Delete User A's creation
        db.generationDao().deleteGeneration(entityA.id, userAId)
        val userAAfterDelete = db.generationDao().getGenerationsForUser(userAId).first()
        assertEquals(0, userAAfterDelete.size)
    }

    @Test
    fun testLocalImageEditorOperations() = runBlocking {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val rotated = localImageEditor.rotateBitmap(source, 90f)
        assertEquals(100, rotated.width)
        assertEquals(100, rotated.height)

        val flipped = localImageEditor.flipBitmap(source, horizontal = true)
        assertEquals(100, flipped.width)

        val filtered = localImageEditor.applyFilter(source, ImageFilterType.GRAYSCALE)
        assertNotNull(filtered)
    }

    @Test
    fun testUpscaleImageOperation() = runBlocking {
        val source = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val upscaleResult = creationRouter.routeUpscale(source, userId = "user_test_upscale")
        assertTrue(upscaleResult.isSuccess)
        val upscaled = upscaleResult.getOrThrow()
        assertEquals(256, upscaled.width)
        assertEquals(256, upscaled.height)
    }

    @Test
    fun testVariationsGeneration() = runBlocking {
        val original = CreationResponse(
            assetId = UUID.randomUUID().toString(),
            assetUri = "file:///dummy.png",
            prompt = "Cyberpunk street in Chennai",
            provider = "Local Engine",
            model = "nila-procedural-v2-local",
            assetType = AssetType.IMAGE_2D
        )
        val varResult = creationRouter.routeVariations(original, count = 2, userId = "user_test_var")
        assertTrue(varResult.isSuccess)
        val list = varResult.getOrThrow()
        assertEquals(2, list.size)
        assertTrue(list[0].prompt.contains("variation 1"))
        assertTrue(list[1].prompt.contains("variation 2"))
    }

    @Test
    fun testThreeDMeshParsing() {
        val sampleObj = """
            # Test OBJ
            v 0.0 1.0 0.0
            v -1.0 -1.0 0.0
            v 1.0 -1.0 0.0
            f 1 2 3
        """.trimIndent()
        val parsed = com.example.ui.threed.ThreeDMeshEngine.parseObjString(sampleObj, name = "Triangle")
        assertEquals(3, parsed.vertexCount)
        assertEquals(1, parsed.faceCount)
        assertEquals("Triangle", parsed.name)
    }
}
