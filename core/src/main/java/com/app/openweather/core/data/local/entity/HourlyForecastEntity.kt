package com.app.openweather.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hourly_forecast")
data class HourlyForecastEntity(
    @PrimaryKey val id: String,       // "$cityKey-$dt"
    val cityKey: String,
    val dt: Long,
    val temp: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val pop: Double,
    val description: String,
    val iconCode: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
