package com.javis.os.data.remote.api

import com.javis.os.data.remote.dto.TtsRequest
import com.javis.os.data.remote.dto.VoicesResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ElevenLabsApi {
    @POST("text-to-speech/{voiceId}")
    @Headers("Accept: audio/mpeg")
    suspend fun textToSpeech(
        @Header("xi-api-key") apiKey: String,
        @Path("voiceId") voiceId: String,
        @Body request: TtsRequest
    ): Response<ResponseBody>

    @GET("voices")
    suspend fun getVoices(
        @Header("xi-api-key") apiKey: String
    ): Response<VoicesResponse>
}
