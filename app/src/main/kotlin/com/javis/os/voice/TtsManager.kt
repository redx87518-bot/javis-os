package com.javis.os.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.javis.os.data.remote.api.ElevenLabsApi
import com.javis.os.data.remote.dto.TtsRequest
import com.javis.os.data.remote.dto.VoiceSettings
import com.javis.os.util.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class TtsState {
    object Idle : TtsState()
    object Speaking : TtsState()
    object Loading : TtsState()
}

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val elevenLabsApi: ElevenLabsApi,
    private val prefs: PreferencesManager
) {
    private var androidTts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state

    private var ttsReady = false

    init {
        initAndroidTts()
    }

    private fun initAndroidTts() {
        androidTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                androidTts?.language = Locale.getDefault()
                androidTts?.setSpeechRate(prefs.getTtsSpeed())
                ttsReady = true
                Log.i("TTS", "Android TTS ready")
            } else {
                Log.e("TTS", "Android TTS init failed: $status")
            }
        }
    }

    suspend fun speak(text: String) {
        if (text.isBlank()) return
        val provider = prefs.getTtsProvider()
        val elevenLabsKey = prefs.getElevenLabsApiKey()

        if (provider == "elevenlabs" && elevenLabsKey.isNotBlank()) {
            speakWithElevenLabs(text, elevenLabsKey)
        } else {
            speakWithAndroid(text)
        }
    }

    private suspend fun speakWithElevenLabs(text: String, apiKey: String) {
        _state.value = TtsState.Loading
        withContext(Dispatchers.IO) {
            try {
                val voiceId = prefs.getElevenLabsVoiceId().ifBlank { "EXAVITQu4vr4xnSDxMaL" }
                Log.d("TTS", "ElevenLabs: voiceId=$voiceId, text=${text.take(50)}")

                val response = elevenLabsApi.textToSpeech(
                    apiKey = apiKey,
                    voiceId = voiceId,
                    request = TtsRequest(
                        text = text,
                        modelId = "eleven_multilingual_v2",
                        voiceSettings = VoiceSettings(
                            stability = 0.5f,
                            similarityBoost = 0.75f,
                            style = 0.3f,
                            useSpeakerBoost = true
                        )
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val bytes = body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        Log.i("TTS", "ElevenLabs returned ${bytes.size} bytes")
                        playAudioBytes(bytes)
                    } else {
                        Log.w("TTS", "ElevenLabs returned empty body, falling back")
                        withContext(Dispatchers.Main) { speakWithAndroid(text) }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.w("TTS", "ElevenLabs HTTP ${response.code()}: $errorBody — falling back")
                    withContext(Dispatchers.Main) { speakWithAndroid(text) }
                }
            } catch (e: Exception) {
                Log.e("TTS", "ElevenLabs exception: ${e.message}")
                withContext(Dispatchers.Main) { speakWithAndroid(text) }
            }
        }
    }

    private fun playAudioBytes(bytes: ByteArray) {
        val tempFile = File(context.cacheDir, "javis_tts_${System.currentTimeMillis()}.mp3")
        try {
            FileOutputStream(tempFile).use { it.write(bytes) }

            stopSpeaking()
            _state.value = TtsState.Speaking

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    _state.value = TtsState.Idle
                    tempFile.delete()
                    Log.d("TTS", "ElevenLabs playback completed")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("TTS", "MediaPlayer error: what=$what extra=$extra")
                    _state.value = TtsState.Idle
                    tempFile.delete()
                    false
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("TTS", "Playback error: ${e.message}")
            _state.value = TtsState.Idle
            tempFile.delete()
        }
    }

    private fun speakWithAndroid(text: String) {
        if (!ttsReady) {
            Log.w("TTS", "Android TTS not ready, reinitializing")
            initAndroidTts()
            return
        }
        stopMediaPlayer()
        _state.value = TtsState.Speaking
        androidTts?.setSpeechRate(prefs.getTtsSpeed())
        val utteranceId = "javis_${System.currentTimeMillis()}"

        androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _state.value = TtsState.Speaking }
            override fun onDone(utteranceId: String?) { _state.value = TtsState.Idle }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { _state.value = TtsState.Idle }
            override fun onError(utteranceId: String?, errorCode: Int) { _state.value = TtsState.Idle }
        })

        val result = androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            Log.e("TTS", "Android TTS speak() returned ERROR")
            _state.value = TtsState.Idle
        }
    }

    fun stopSpeaking() {
        stopMediaPlayer()
        androidTts?.stop()
        _state.value = TtsState.Idle
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun isSpeaking(): Boolean = _state.value is TtsState.Speaking

    fun destroy() {
        stopSpeaking()
        androidTts?.shutdown()
        androidTts = null
    }
}
