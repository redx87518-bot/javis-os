package com.javis.os.agent

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.javis.os.domain.repository.MemoryRepository
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

// Open-Meteo is free, no API key needed
interface OpenMeteoApi {
    @GET("forecast")
    suspend fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m",
        @Query("temperature_unit") unit: String = "celsius"
    ): Response<WeatherResponse>
}

data class WeatherResponse(
    val current: CurrentWeather? = null
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double = 0.0,
    @SerializedName("relative_humidity_2m") val humidity: Int = 0,
    @SerializedName("weather_code") val code: Int = 0,
    @SerializedName("wind_speed_10m") val windSpeed: Double = 0.0
)

@Singleton
class WeatherAgent @Inject constructor(
    private val memoryRepository: MemoryRepository
) {
    // Default to Kano, Nigeria coordinates (common for the user's location)
    // Will use remembered location if available
    private val defaultLat = 12.0022
    private val defaultLon = 8.5920

    suspend fun getWeatherResponse(): String {
        return try {
            // Try to get user's remembered location
            val locationStr = memoryRepository.recall("location")
            val (lat, lon, locationName) = resolveLocation(locationStr)

            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/v1/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()

            val api = retrofit.create(OpenMeteoApi::class.java)
            val response = api.getForecast(lat = lat, lon = lon)

            if (response.isSuccessful) {
                val weather = response.body()?.current
                if (weather != null) {
                    val desc = weatherCodeToDescription(weather.code)
                    val temp = weather.temperature.toInt()
                    val humidity = weather.humidity
                    val wind = weather.windSpeed.toInt()
                    buildWeatherMessage(locationName, temp, desc, humidity, wind)
                } else {
                    "I couldn't get the weather data right now. Try again in a moment."
                }
            } else {
                "Weather service is unavailable right now."
            }
        } catch (e: Exception) {
            Log.e("WeatherAgent", "Weather error: ${e.message}")
            "I couldn't fetch the weather. Make sure you're connected to the internet."
        }
    }

    private fun buildWeatherMessage(location: String, temp: Int, desc: String, humidity: Int, wind: Int): String {
        val feel = when {
            temp >= 38 -> "It's very hot — stay hydrated!"
            temp >= 32 -> "It's quite warm out there."
            temp >= 25 -> "It's a pleasant day."
            temp >= 18 -> "It's a bit cool."
            else -> "It's cold — layer up!"
        }
        return "Right now in $location it's $temp°C with $desc. Humidity is $humidity% and wind speed is ${wind}km/h. $feel"
    }

    private fun resolveLocation(locationStr: String?): Triple<Double, Double, String> {
        if (locationStr.isNullOrBlank()) return Triple(defaultLat, defaultLon, "Kano")
        // Simple city lookup table for common Nigerian cities
        val cities = mapOf(
            "kano" to Triple(12.0022, 8.5920, "Kano"),
            "lagos" to Triple(6.5244, 3.3792, "Lagos"),
            "abuja" to Triple(9.0579, 7.4951, "Abuja"),
            "ibadan" to Triple(7.3775, 3.9470, "Ibadan"),
            "kaduna" to Triple(10.5264, 7.4384, "Kaduna"),
            "port harcourt" to Triple(4.8156, 7.0498, "Port Harcourt"),
            "zaria" to Triple(11.0855, 7.7199, "Zaria"),
            "maiduguri" to Triple(11.8311, 13.1504, "Maiduguri"),
            "sokoto" to Triple(13.0059, 5.2476, "Sokoto"),
        )
        val lower = locationStr.lowercase()
        for ((key, value) in cities) {
            if (lower.contains(key)) return value
        }
        return Triple(defaultLat, defaultLon, locationStr.replaceFirstChar { it.uppercase() })
    }

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0 -> "clear sky"
        1, 2, 3 -> "partly cloudy"
        45, 48 -> "foggy"
        51, 53, 55 -> "drizzle"
        61, 63, 65 -> "rain"
        71, 73, 75 -> "snow"
        80, 81, 82 -> "rain showers"
        95 -> "thunderstorms"
        96, 99 -> "thunderstorms with hail"
        else -> "mixed conditions"
    }
}
