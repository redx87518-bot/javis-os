package com.javis.os.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TtsRequest(
    val text: String,
    @SerializedName("model_id") val modelId: String = "eleven_multilingual_v2",
    @SerializedName("voice_settings") val voiceSettings: VoiceSettings = VoiceSettings()
)

data class VoiceSettings(
    val stability: Float = 0.5f,
    @SerializedName("similarity_boost") val similarityBoost: Float = 0.75f,
    val style: Float = 0.3f,
    @SerializedName("use_speaker_boost") val useSpeakerBoost: Boolean = true
)

data class VoiceModel(
    @SerializedName("voice_id") val voiceId: String,
    val name: String,
    val category: String = ""
)

data class VoicesResponse(
    val voices: List<VoiceModel> = emptyList()
)
