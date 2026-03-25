package com.packapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val locationKey: String,
    val locationLabel: String,
    val fetchedAt: Long,
    val expiresAt: Long,
    val latitude: Double,
    val longitude: Double,
    val temperatureMinC: Double,
    val temperatureMaxC: Double,
    val precipitationProbabilityMax: Int
)
