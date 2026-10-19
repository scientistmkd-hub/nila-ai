package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.OrbStateController
import com.example.ai.vision.FaceAndEyeEngine
import com.example.ai.vision.GestureEngine
import com.example.core.command.CommandResult
import com.example.core.command.LocalCommandRouter
import com.example.core.model.EyeInteractionState
import com.example.core.model.HandGesture
import com.example.core.model.OrbState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NilaMultimodalUnitTest {

    @Test
    fun `tanglish camera command routes to camera vision`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val router = LocalCommandRouter(context)
        val result = router.evaluate("nila camera open pannu")
        assertTrue(result is CommandResult.Handled)
        val handled = result as CommandResult.Handled
        assertEquals("camera", handled.navigationTarget)
    }

    @Test
    fun `tanglish photo capture command triggers photo flag`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val router = LocalCommandRouter(context)
        val result = router.evaluate("nila photo edu")
        assertTrue(result is CommandResult.Handled)
        val handled = result as CommandResult.Handled
        assertTrue(handled.shouldTakePhoto)
        assertEquals("camera", handled.navigationTarget)
    }

    @Test
    fun `tanglish stop command requests speech interruption`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val router = LocalCommandRouter(context)
        val result = router.evaluate("nila stop")
        assertTrue(result is CommandResult.Handled)
        val handled = result as CommandResult.Handled
        assertTrue(handled.shouldStopSpeech)
    }

    @Test
    fun `tanglish time command returns current time`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val router = LocalCommandRouter(context)
        val result = router.evaluate("nila time enna")
        assertTrue(result is CommandResult.Handled)
        val handled = result as CommandResult.Handled
        assertTrue(handled.reply.contains("current time is"))
    }

    @Test
    fun `eye attention evaluates looking toward device for small yaw angles`() {
        val lookingToward = FaceAndEyeEngine.calculateEyeAttention(eulerY = 2.5f, leftEyeOpen = 0.85f, rightEyeOpen = 0.88f)
        assertEquals(EyeInteractionState.LOOKING_TOWARD_DEVICE, lookingToward)

        val lookingAway = FaceAndEyeEngine.calculateEyeAttention(eulerY = 32.0f, leftEyeOpen = 0.85f, rightEyeOpen = 0.88f)
        assertEquals(EyeInteractionState.LOOKING_AWAY, lookingAway)

        val eyesClosed = FaceAndEyeEngine.calculateEyeAttention(eulerY = 0f, leftEyeOpen = 0.1f, rightEyeOpen = 0.1f)
        assertEquals(EyeInteractionState.LOOKING_AWAY, eyesClosed)
    }

    @Test
    fun `gesture engine initializes with clean state and default threshold`() {
        val engine = GestureEngine(confidenceThreshold = 0.85f, debounceCooldownMs = 1500L)
        engine.reset()
        // Confidence threshold can be updated dynamically
        engine.setConfidenceThreshold(0.80f)
        engine.setCooldown(2000L)
    }

    @Test
    fun `orb state controller transitions correctly between camera and voice`() {
        val controller = OrbStateController()
        assertEquals(OrbState.IDLE, controller.currentOrbState.value)

        // Turn on camera
        controller.setCameraActive(true)
        assertEquals(OrbState.CAMERA_ACTIVE, controller.currentOrbState.value)

        // Eye contact detected
        controller.onEyeAttentionChanged(true)
        assertEquals(OrbState.EYE_CONTACT, controller.currentOrbState.value)

        // User speaks
        controller.onVoiceState(isListening = true, isSpeaking = false, isThinking = false)
        assertEquals(OrbState.LISTENING, controller.currentOrbState.value)

        // Speech interrupted
        controller.onSpeechInterrupted()
        assertEquals(OrbState.CAMERA_ACTIVE, controller.currentOrbState.value)

        // Camera closed
        controller.setCameraActive(false)
        assertEquals(OrbState.IDLE, controller.currentOrbState.value)

        // Offline mode
        controller.setOffline(true)
        assertEquals(OrbState.OFFLINE, controller.currentOrbState.value)
    }
}
