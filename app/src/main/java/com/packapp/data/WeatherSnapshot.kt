package com.packapp.data

data class WeatherSnapshot(
    val locationLabel: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureMinC: Double,
    val temperatureMaxC: Double,
    val precipitationProbabilityMax: Int,
    val fetchedAt: Long,
    val expiresAt: Long,
    val isFromCache: Boolean
)
