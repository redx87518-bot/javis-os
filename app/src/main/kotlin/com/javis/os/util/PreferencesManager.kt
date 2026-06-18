package com.javis.os.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "javis_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val KEY_GROQ_MODEL = stringPreferencesKey("groq_model")
        val KEY_DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")
        val KEY_ELEVENLABS_API_KEY = stringPreferencesKey("elevenlabs_api_key")
        val KEY_ELEVENLABS_VOICE_ID = stringPreferencesKey("elevenlabs_voice_id")
        val KEY_TTS_PROVIDER = stringPreferencesKey("tts_provider")
        val KEY_TTS_SPEED = floatPreferencesKey("tts_speed")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val KEY_VOICE_ACTIVATION = booleanPreferencesKey("voice_activation")
    }

    fun getAiProvider(): String = runBlocking {
        dataStore.data.first()[KEY_AI_PROVIDER] ?: "groq"
    }

    suspend fun setAiProvider(provider: String) {
        dataStore.edit { it[KEY_AI_PROVIDER] = provider }
    }

    fun getGroqApiKey(): String = runBlocking {
        dataStore.data.first()[KEY_GROQ_API_KEY] ?: ""
    }

    suspend fun setGroqApiKey(key: String) {
        dataStore.edit { it[KEY_GROQ_API_KEY] = key }
    }

    fun getGroqModel(): String = runBlocking {
        dataStore.data.first()[KEY_GROQ_MODEL] ?: "llama3-70b-8192"
    }

    suspend fun setGroqModel(model: String) {
        dataStore.edit { it[KEY_GROQ_MODEL] = model }
    }

    fun getDeepSeekApiKey(): String = runBlocking {
        dataStore.data.first()[KEY_DEEPSEEK_API_KEY] ?: ""
    }

    suspend fun setDeepSeekApiKey(key: String) {
        dataStore.edit { it[KEY_DEEPSEEK_API_KEY] = key }
    }

    fun getElevenLabsApiKey(): String = runBlocking {
        dataStore.data.first()[KEY_ELEVENLABS_API_KEY] ?: ""
    }

    suspend fun setElevenLabsApiKey(key: String) {
        dataStore.edit { it[KEY_ELEVENLABS_API_KEY] = key }
    }

    fun getElevenLabsVoiceId(): String = runBlocking {
        dataStore.data.first()[KEY_ELEVENLABS_VOICE_ID] ?: "EXAVITQu4vr4xnSDxMaL"
    }

    suspend fun setElevenLabsVoiceId(id: String) {
        dataStore.edit { it[KEY_ELEVENLABS_VOICE_ID] = id }
    }

    fun getTtsProvider(): String = runBlocking {
        dataStore.data.first()[KEY_TTS_PROVIDER] ?: "android"
    }

    suspend fun setTtsProvider(provider: String) {
        dataStore.edit { it[KEY_TTS_PROVIDER] = provider }
    }

    fun getTtsSpeed(): Float = runBlocking {
        dataStore.data.first()[KEY_TTS_SPEED] ?: 1.0f
    }

    suspend fun setTtsSpeed(speed: Float) {
        dataStore.edit { it[KEY_TTS_SPEED] = speed }
    }

    fun getUserName(): String = runBlocking {
        dataStore.data.first()[KEY_USER_NAME] ?: ""
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[KEY_USER_NAME] = name }
    }

    fun isOfflineMode(): Boolean = runBlocking {
        dataStore.data.first()[KEY_OFFLINE_MODE] ?: false
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        dataStore.edit { it[KEY_OFFLINE_MODE] = enabled }
    }

    /** Returns true if at least one AI API key has been entered. */
    fun hasAnyApiKey(): Boolean = getGroqApiKey().isNotBlank() || getDeepSeekApiKey().isNotBlank()
}
