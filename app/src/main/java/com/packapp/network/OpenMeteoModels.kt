package com.packapp.network

import com.squareup.moshi.Json

data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingResult>?
)

data class GeocodingResult(
    @Json(name = "name") val name: String,
    @Json(name = "country") val country: String?,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double
)

data class ForecastResponse(
    @Json(name = "daily") val daily: DailyForecast?
)

data class DailyForecast(
    @Json(name = "time") val time: List<String>?,
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double>?,
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double>?,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>?
)
