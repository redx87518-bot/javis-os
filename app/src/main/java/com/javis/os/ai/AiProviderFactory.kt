package com.javis.os.ai

object AiProviderFactory {
    fun create(provider: String, apiKey: String): AiProvider {
        return when (provider.lowercase()) {
            "openai" -> OpenAiProvider(apiKey)
            else -> GeminiProvider(apiKey)
        }
    }
}
