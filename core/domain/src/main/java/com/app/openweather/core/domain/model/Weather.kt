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

data class HourlyForecast(
    val dt: Long,
    val temp: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val pop: Double,
    val description: String,
    val iconCode: String,
)

data class DailyForecast(
    val date: Long,
    val tempMin: Double,
    val tempMax: Double,
    val description: String,
    val iconCode: String,
    val humidity: Int,
    val windSpeed: Double,
    val pop: Double,
)

data class Forecast(
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
)

data class City(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
)

data class SavedCity(
    val id: String,          // "$name,$country"
    val name: String,
    val country: String,
    val state: String?,
    val lat: Double,
    val lon: Double,
    val isFavorite: Boolean,
    val savedAt: Long,
)
