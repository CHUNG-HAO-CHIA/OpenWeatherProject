package com.app.openweather.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_forecast")
data class DailyForecastEntity(
    @PrimaryKey val id: String,        // "$lat,$lon-$date"
    val cityKey: String,               // "$lat,$lon"
    val date: Long,
    val tempMin: Double,
    val tempMax: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double,
    val pop: Double = 0.0,
)
