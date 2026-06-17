package com.javis.os.features.wakeword

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.javis.os.MainActivity

class WakeWordService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var restartAttempts = 0
    private val maxRestartAttempts = 5

    private val wakeWords = listOf(
        "hey javis", "javis", "hey jarvis", "ok javis",
        "hi javis", "activate javis", "javis wake up"
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            startListening()
        }
    }

    private fun startListening() {
        if (isListening) return
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListening = true; restartAttempts = 0 }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(error: Int) {
                    isListening = false
                    if (restartAttempts < maxRestartAttempts) {
                        restartAttempts++
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            startListening()
                        }, 2000L)
                    }
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.lowercase() ?: ""
                    if (wakeWords.any { spoken.contains(it) }) {
                        activateJavis()
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        startListening()
                    }, 500L)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.lowercase() ?: return
                    if (wakeWords.any { partial.contains(it) }) {
                        speechRecognizer?.stopListening()
                        activateJavis()
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) { /* ignore */ }
    }

    private fun activateJavis() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.javis.os.ACTIVATE_VOICE"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
