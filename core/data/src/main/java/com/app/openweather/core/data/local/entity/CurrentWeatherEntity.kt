package com.app.openweather.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_weather")
data class CurrentWeatherEntity(
    @PrimaryKey val cityKey: String,   // "$lat,$lon"
    val cityName: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val description: String,
    val iconCode: String,
    val sunrise: Long,
    val sunset: Long,
    val localizedNames: String = "",   // JSON: {"name:en":"Tokyo","name:ja":"東京",...}
    val updatedAt: Long = System.currentTimeMillis(),
)
