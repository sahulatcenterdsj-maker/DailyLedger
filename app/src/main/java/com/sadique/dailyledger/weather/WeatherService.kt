package com.sadique.dailyledger.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

data class WeatherSnapshot(
    val city: String,
    val temperature: String,
    val condition: String,
    val fetchedAt: Long = System.currentTimeMillis(),
)

class WeatherService {
    suspend fun currentForCity(query: String): WeatherSnapshot = withContext(Dispatchers.IO) {
        val cityQuery = query.trim()
        require(cityQuery.length in 2..120) { "Enter a city name first." }

        val encoded = URLEncoder.encode(cityQuery.substringBefore(",").trim(), StandardCharsets.UTF_8.toString())
        val geo = getJson(
            "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=en&format=json"
        )
        val results = geo.optJSONArray("results")
        require(results != null && results.length() > 0) { "City not found. Try a more specific name, for example Gujranwala, Pakistan." }
        val place = results.getJSONObject(0)
        val latitude = place.getDouble("latitude")
        val longitude = place.getDouble("longitude")
        val name = place.optString("name").ifBlank { cityQuery }
        val admin1 = place.optString("admin1")
        val country = place.optString("country")
        val cityLabel = listOf(name, admin1, country)
            .filter { it.isNotBlank() }
            .distinct()
            .take(2)
            .joinToString(", ")

        val forecast = getJson(
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code&timezone=auto"
        )
        val current = forecast.optJSONObject("current")
            ?: throw IllegalStateException("Weather service returned no current conditions.")
        val temp = current.getDouble("temperature_2m").roundToInt()
        val weatherCode = current.optInt("weather_code", -1)

        WeatherSnapshot(
            city = cityLabel,
            temperature = "$temp°C",
            condition = weatherLabel(weatherCode),
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "DailyLedger-Android")
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("Weather service is temporarily unavailable.")
            connection.inputStream.use { stream ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = stream.read(buffer); if (count < 0) break
                    require(out.size() + count <= 128 * 1024) { "Weather response is too large." }
                    out.write(buffer, 0, count)
                }
                JSONObject(out.toString("UTF-8"))
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun weatherLabel(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Storm with hail"
        else -> "Current weather"
    }
}
