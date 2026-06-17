package com.javis.os.features.weather

import android.location.Geocoder
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class WeatherData(
    val location: String,
    val tempC: Int,
    val tempF: Int,
    val description: String,
    val humidity: Int,
    val windKph: Int,
    val feelsLikeC: Int
)

@Singleton
class WeatherService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getWeather(location: String = "auto"): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            val query = if (location == "auto") "auto:ip" else location.replace(" ", "+")
            val url = "https://wttr.in/$query?format=j1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "JAVIS-OS-Android/1.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("No weather data"))

            val wttr = gson.fromJson(body, WttrResponse::class.java)
            val current = wttr.currentCondition?.firstOrNull()
                ?: return@withContext Result.failure(Exception("No current conditions"))

            val area = wttr.nearestArea?.firstOrNull()
            val cityName = area?.areaName?.firstOrNull()?.value ?: location
            val country = area?.country?.firstOrNull()?.value ?: ""

            val data = WeatherData(
                location = if (country.isNotBlank()) "$cityName, $country" else cityName,
                tempC = current.tempC?.toIntOrNull() ?: 0,
                tempF = current.tempF?.toIntOrNull() ?: 0,
                description = current.weatherDesc?.firstOrNull()?.value ?: "Clear",
                humidity = current.humidity?.toIntOrNull() ?: 0,
                windKph = current.windspeedKmph?.toIntOrNull() ?: 0,
                feelsLikeC = current.feelsLikeC?.toIntOrNull() ?: 0
            )
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatWeatherResponse(data: WeatherData): String {
        val emoji = when {
            data.description.contains("sunny", true) || data.description.contains("clear", true) -> "☀️"
            data.description.contains("cloud", true) || data.description.contains("overcast", true) -> "☁️"
            data.description.contains("rain", true) || data.description.contains("drizzle", true) -> "🌧️"
            data.description.contains("snow", true) -> "❄️"
            data.description.contains("storm", true) || data.description.contains("thunder", true) -> "⛈️"
            data.description.contains("fog", true) || data.description.contains("mist", true) -> "🌫️"
            else -> "🌤️"
        }
        return "In ${data.location}, it's ${data.tempC}°C (${data.tempF}°F). " +
                "${data.description}. Feels like ${data.feelsLikeC}°C. " +
                "Humidity ${data.humidity}%, wind ${data.windKph} km/h."
    }

    private data class WttrResponse(
        @SerializedName("current_condition") val currentCondition: List<CurrentCondition>?,
        @SerializedName("nearest_area") val nearestArea: List<NearestArea>?
    )

    private data class CurrentCondition(
        @SerializedName("temp_C") val tempC: String?,
        @SerializedName("temp_F") val tempF: String?,
        @SerializedName("weatherDesc") val weatherDesc: List<WttrValue>?,
        val humidity: String?,
        @SerializedName("windspeedKmph") val windspeedKmph: String?,
        @SerializedName("FeelsLikeC") val feelsLikeC: String?
    )

    private data class NearestArea(
        val areaName: List<WttrValue>?,
        val country: List<WttrValue>?
    )

    private data class WttrValue(val value: String?)
}
