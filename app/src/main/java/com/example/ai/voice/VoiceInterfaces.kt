package com.example.ai.voice

import com.example.core.model.VoiceState
import kotlinx.coroutines.flow.StateFlow

/**
 * Clean architecture interface for Text-to-Speech engines.
 * Implementations can be Android TTS, local neural TTS, or cloud TTS.
 */
interface TextToSpeechEngine {
    val isSpeaking: StateFlow<Boolean>
    fun setLanguage(languageName: String)
    fun speak(text: String, rate: Float = 1.0f, pitch: Float = 1.0f, languageName: String = "English")
    fun stop()
    fun shutdown()
}

/**
 * Clean architecture interface for Speech-to-Text engines.
 * Implementations can be Android SpeechRecognizer, on-device Whisper, or cloud STT.
 */
interface SpeechToTextEngine {
    val voiceState: StateFlow<VoiceState>
    val isRecognitionAvailable: Boolean
    fun startListening(
        language: String = "English",
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    )
    fun stopListening()
    fun release()
}

/**
 * Clean architecture interface for Wake Word detection ("Nila").
 */
interface WakeWordEngine {
    val isListening: StateFlow<Boolean>
    val isWakeWordActive: Boolean
    fun startListening(onWakeWordDetected: (String) -> Unit, onError: (String) -> Unit = {})
    fun stopListening()
    fun release()
}

/**
 * Composite voice engine coordinating STT, TTS, and Wake-word processing.
 */
interface VoiceEngine {
    val sttEngine: SpeechToTextEngine
    val ttsEngine: TextToSpeechEngine
    val wakeWordEngine: WakeWordEngine
}
