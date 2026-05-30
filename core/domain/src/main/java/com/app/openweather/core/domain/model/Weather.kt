package com.app.openweather.core.domain.model

data class CurrentWeather(
    val cityName: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val description: String,
    val iconCode: String,
    val sunrise: Long,
    val sunset: Long,
)

data class DailyForecast(
    val date: Long,
    val tempMin: Double,
    val tempMax: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double,
)

data class City(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
)
