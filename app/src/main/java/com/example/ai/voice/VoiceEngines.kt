package com.example.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.core.model.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AndroidTtsEngine(context: Context) : TextToSpeechEngine, TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    override fun setLanguage(languageName: String) {
        if (!isInitialized || tts == null) return
        val targetLocale = when (languageName.lowercase(Locale.ROOT)) {
            "tamil", "tanglish" -> Locale("ta", "IN")
            else -> Locale.US
        }
        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fall back to default device locale
            tts?.language = Locale.getDefault()
        }
    }

    override fun speak(text: String, rate: Float, pitch: Float, languageName: String) {
        if (!isInitialized || tts == null) return
        setLanguage(languageName)
        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)
        val utteranceId = "nila_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

class AndroidSpeechRecognizerEngine(private val context: Context) : SpeechToTextEngine {
    private var recognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    override val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    override val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun startListening(
        language: String,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isRecognitionAvailable) {
            _voiceState.value = VoiceState.UNAVAILABLE
            onError("Speech recognition is not available on this device.")
            return
        }

        stopListening()

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speechRecognizer

        val targetLocale = when (language.lowercase(Locale.ROOT)) {
            "tamil" -> Locale("ta", "IN")
            else -> Locale.getDefault()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.LISTENING
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.PROCESSING
            }

            override fun onError(error: Int) {
                _voiceState.value = VoiceState.ERROR
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out."
                    SpeechRecognizer.ERROR_NETWORK -> "Network issue during speech recognition."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    else -> "Speech recognition error code: $error"
                }
                onError(message)
                release()
            }

            override fun onResults(results: Bundle?) {
                _voiceState.value = VoiceState.IDLE
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull()?.trim() ?: ""
                onFinalResult(spoken)
                release()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim() ?: ""
                if (partial.isNotBlank()) {
                    onPartialResult(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.ERROR
            onError("Failed to initiate voice input: ${e.localizedMessage}")
            release()
        }
    }

    override fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (_: Exception) {}
        release()
    }

    override fun release() {
        try {
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
        if (_voiceState.value == VoiceState.LISTENING || _voiceState.value == VoiceState.PROCESSING) {
            _voiceState.value = VoiceState.IDLE
        }
    }
}

/**
 * On-device keyword / wake-word detection engine ("Nila").
 * Only activates when explicitly enabled and permissions are granted.
 */
class AndroidWakeWordEngine(private val context: Context) : WakeWordEngine {
    private var recognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var shouldKeepListening = false
    private var callback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    override val isWakeWordActive: Boolean
        get() = shouldKeepListening && _isListening.value

    override fun startListening(onWakeWordDetected: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Wake-word engine is unavailable on this device.")
            return
        }

        shouldKeepListening = true
        callback = onWakeWordDetected
        errorCallback = onError
        listenLoop()
    }

    private fun listenLoop() {
        if (!shouldKeepListening) return

        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Prefer offline recognition for privacy and responsiveness where supported
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    _isListening.value = false
                    // If still enabled and not permanently denied, gently restart loop
                    if (shouldKeepListening && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            listenLoop()
                        }, 1000)
                    } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        shouldKeepListening = false
                        errorCallback?.invoke("Microphone permission required for Wake Word.")
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    checkTranscriptForWakeWord(matches?.firstOrNull())
                    if (shouldKeepListening) {
                        listenLoop()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    checkTranscriptForWakeWord(matches?.firstOrNull())
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            errorCallback?.invoke("Could not start wake word: ${e.localizedMessage}")
        }
    }

    private fun checkTranscriptForWakeWord(transcript: String?) {
        if (transcript.isNullOrBlank()) return
        val clean = transcript.lowercase(Locale.ROOT)
        if (clean.contains("nila") || clean.contains("hey nila") || clean.contains("hi nila") || clean.contains("hello nila")) {
            callback?.invoke("Nila")
        }
    }

    override fun stopListening() {
        shouldKeepListening = false
        _isListening.value = false
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }

    override fun release() {
        stopListening()
    }
}

/**
 * Composite voice engine satisfying Section 2 architecture specification.
 */
class NilaVoiceEngine(
    context: Context,
    override val sttEngine: SpeechToTextEngine = AndroidSpeechRecognizerEngine(context),
    override val ttsEngine: TextToSpeechEngine = AndroidTtsEngine(context),
    override val wakeWordEngine: WakeWordEngine = AndroidWakeWordEngine(context)
) : VoiceEngine
