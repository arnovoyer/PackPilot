package com.packapp.network

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoGeocodingService {
    @GET("v1/search")
    suspend fun searchLocation(
        @Query("name") name: String,
        @Query("count") count: Int = 1,
        @Query("language") language: String = "de",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

interface OpenMeteoForecastService {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "temperature_2m_min,temperature_2m_max,precipitation_probability_max",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("timezone") timezone: String = "auto"
    ): ForecastResponse
}
