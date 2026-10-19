package com.example.ai

import com.example.core.model.HandGesture
import com.example.core.model.OrbState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OrbStateController {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentOrbState = MutableStateFlow(OrbState.IDLE)
    val currentOrbState: StateFlow<OrbState> = _currentOrbState.asStateFlow()

    private var gestureResetJob: Job? = null
    private var isCameraRunning = false
    private var isOffline = false

    fun setOffline(offline: Boolean) {
        this.isOffline = offline
        if (offline) {
            _currentOrbState.value = OrbState.OFFLINE
        } else if (_currentOrbState.value == OrbState.OFFLINE) {
            _currentOrbState.value = OrbState.IDLE
        }
    }

    fun setCameraActive(active: Boolean) {
        this.isCameraRunning = active
        if (active) {
            if (_currentOrbState.value != OrbState.GESTURE_DETECTED && _currentOrbState.value != OrbState.SPEAKING) {
                _currentOrbState.value = OrbState.CAMERA_ACTIVE
            }
        } else {
            if (_currentOrbState.value == OrbState.CAMERA_ACTIVE || _currentOrbState.value == OrbState.EYE_CONTACT) {
                _currentOrbState.value = if (isOffline) OrbState.OFFLINE else OrbState.IDLE
            }
        }
    }

    fun onGestureDetected(gesture: HandGesture) {
        if (gesture == HandGesture.UNKNOWN) return
        gestureResetJob?.cancel()

        _currentOrbState.value = OrbState.GESTURE_DETECTED

        gestureResetJob = scope.launch {
            delay(1200)
            if (isCameraRunning) {
                _currentOrbState.value = OrbState.CAMERA_ACTIVE
            } else {
                _currentOrbState.value = if (isOffline) OrbState.OFFLINE else OrbState.IDLE
            }
        }
    }

    fun onEyeAttentionChanged(isLookingAtDevice: Boolean) {
        if (!isCameraRunning) return
        if (_currentOrbState.value == OrbState.GESTURE_DETECTED || _currentOrbState.value == OrbState.SPEAKING) return

        if (isLookingAtDevice) {
            _currentOrbState.value = OrbState.EYE_CONTACT
        } else {
            _currentOrbState.value = OrbState.CAMERA_ACTIVE
        }
    }

    fun onVoiceState(isListening: Boolean, isSpeaking: Boolean, isThinking: Boolean) {
        gestureResetJob?.cancel()
        when {
            isListening -> _currentOrbState.value = OrbState.LISTENING
            isThinking -> _currentOrbState.value = OrbState.THINKING
            isSpeaking -> _currentOrbState.value = OrbState.SPEAKING
            isCameraRunning -> _currentOrbState.value = OrbState.CAMERA_ACTIVE
            isOffline -> _currentOrbState.value = OrbState.OFFLINE
            else -> _currentOrbState.value = OrbState.IDLE
        }
    }

    fun onSpeechInterrupted() {
        gestureResetJob?.cancel()
        _currentOrbState.value = if (isCameraRunning) OrbState.CAMERA_ACTIVE else (if (isOffline) OrbState.OFFLINE else OrbState.IDLE)
    }

    fun release() {
        scope.cancel()
    }
}
