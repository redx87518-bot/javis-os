package com.javis.os.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "javis_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_AI_API_KEY = stringPreferencesKey("ai_api_key")
        val KEY_VOICE_SPEED = floatPreferencesKey("voice_speed")
        val KEY_VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val KEY_VOICE_ENGINE = stringPreferencesKey("voice_engine")
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_BACKGROUND_SERVICE = booleanPreferencesKey("background_service")
        val KEY_SESSION_ID = stringPreferencesKey("session_id")
    }

    val userName: Flow<String> = dataStore.data.map { it[KEY_USER_NAME] ?: "" }
    val aiProvider: Flow<String> = dataStore.data.map { it[KEY_AI_PROVIDER] ?: "gemini" }
    val aiApiKey: Flow<String> = dataStore.data.map { it[KEY_AI_API_KEY] ?: "" }
    val voiceSpeed: Flow<Float> = dataStore.data.map { it[KEY_VOICE_SPEED] ?: 1.0f }
    val voicePitch: Flow<Float> = dataStore.data.map { it[KEY_VOICE_PITCH] ?: 1.0f }
    val voiceEngine: Flow<String> = dataStore.data.map { it[KEY_VOICE_ENGINE] ?: "android_tts" }
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { it[KEY_FIRST_LAUNCH] ?: true }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }
    val backgroundService: Flow<Boolean> = dataStore.data.map { it[KEY_BACKGROUND_SERVICE] ?: true }
    val sessionId: Flow<String> = dataStore.data.map { it[KEY_SESSION_ID] ?: generateSessionId() }

    suspend fun setUserName(name: String) = dataStore.edit { it[KEY_USER_NAME] = name }
    suspend fun setAiProvider(provider: String) = dataStore.edit { it[KEY_AI_PROVIDER] = provider }
    suspend fun setAiApiKey(key: String) = dataStore.edit { it[KEY_AI_API_KEY] = key }
    suspend fun setVoiceSpeed(speed: Float) = dataStore.edit { it[KEY_VOICE_SPEED] = speed }
    suspend fun setVoicePitch(pitch: Float) = dataStore.edit { it[KEY_VOICE_PITCH] = pitch }
    suspend fun setVoiceEngine(engine: String) = dataStore.edit { it[KEY_VOICE_ENGINE] = engine }
    suspend fun setFirstLaunch(first: Boolean) = dataStore.edit { it[KEY_FIRST_LAUNCH] = first }
    suspend fun setNotificationsEnabled(enabled: Boolean) = dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setBackgroundService(enabled: Boolean) = dataStore.edit { it[KEY_BACKGROUND_SERVICE] = enabled }
    suspend fun setSessionId(id: String) = dataStore.edit { it[KEY_SESSION_ID] = id }

    private fun generateSessionId() = System.currentTimeMillis().toString()
}
