package com.javis.os.voice

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.javis.os.data.remote.api.ElevenLabsApi
import com.javis.os.data.remote.dto.TtsRequest
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
                ttsReady = true
            }
        }
    }

    suspend fun speak(text: String) {
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
                val voiceId = prefs.getElevenLabsVoiceId()
                val response = elevenLabsApi.textToSpeech(
                    apiKey = apiKey,
                    voiceId = voiceId,
                    request = TtsRequest(text = text)
                )
                if (response.isSuccessful) {
                    val bytes = response.body()?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        playAudioBytes(bytes)
                        return@withContext
                    }
                }
                // Fallback to android TTS
                Log.w("TTS", "ElevenLabs failed: ${response.code()}, falling back to Android TTS")
                speakWithAndroid(text)
            } catch (e: Exception) {
                Log.e("TTS", "ElevenLabs error: ${e.message}")
                speakWithAndroid(text)
            }
        }
    }

    private fun playAudioBytes(bytes: ByteArray) {
        val tempFile = File(context.cacheDir, "javis_tts_${System.currentTimeMillis()}.mp3")
        FileOutputStream(tempFile).use { it.write(bytes) }

        stopSpeaking()
        _state.value = TtsState.Speaking
        mediaPlayer = MediaPlayer().apply {
            setDataSource(tempFile.absolutePath)
            prepare()
            setOnCompletionListener {
                _state.value = TtsState.Idle
                tempFile.delete()
            }
            setOnErrorListener { _, _, _ ->
                _state.value = TtsState.Idle
                false
            }
            start()
        }
    }

    private fun speakWithAndroid(text: String) {
        if (!ttsReady) {
            initAndroidTts()
            return
        }
        _state.value = TtsState.Speaking
        androidTts?.setSpeechRate(prefs.getTtsSpeed())
        androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "javis_${System.currentTimeMillis()}")
        // Android TTS doesn't have a simple completion callback here, so we approximate
        androidTts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { _state.value = TtsState.Idle }
            override fun onError(utteranceId: String?) { _state.value = TtsState.Idle }
        })
    }

    fun stopSpeaking() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        androidTts?.stop()
        _state.value = TtsState.Idle
    }

    fun isSpeaking(): Boolean = _state.value is TtsState.Speaking

    fun destroy() {
        stopSpeaking()
        androidTts?.shutdown()
    }
}
