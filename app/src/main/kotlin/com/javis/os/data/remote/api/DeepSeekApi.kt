package com.javis.os.data.remote.api

import com.javis.os.data.remote.dto.ChatRequest
import com.javis.os.data.remote.dto.ChatResponse
import retrofit2.Response
import retrofit2.http.*

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
