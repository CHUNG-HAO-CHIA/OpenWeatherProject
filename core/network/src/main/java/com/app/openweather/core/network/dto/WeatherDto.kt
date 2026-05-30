package com.app.openweather.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// data/2.5/weather
@Serializable
data class CurrentWeatherDto(
    @SerialName("name") val name: String,
    @SerialName("main") val main: MainDto,
    @SerialName("weather") val weather: List<WeatherDescDto>,
    @SerialName("wind") val wind: WindDto,
    @SerialName("sys") val sys: SysDto,
)

// data/2.5/forecast  (every 3 h, up to 40 entries / 5 days)
@Serializable
data class ForecastResponseDto(
    @SerialName("list") val list: List<ForecastItemDto>,
)

@Serializable
data class ForecastItemDto(
    @SerialName("dt") val dt: Long,
    @SerialName("main") val main: MainDto,
    @SerialName("weather") val weather: List<WeatherDescDto>,
    @SerialName("wind") val wind: WindDto,
    @SerialName("pop") val pop: Double = 0.0,
    @SerialName("dt_txt") val dtTxt: String,
)

@Serializable
data class MainDto(
    @SerialName("temp") val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    @SerialName("temp_min") val tempMin: Double,
    @SerialName("temp_max") val tempMax: Double,
    @SerialName("humidity") val humidity: Int,
)

@Serializable
data class WindDto(
    @SerialName("speed") val speed: Double,
)

@Serializable
data class SysDto(
    @SerialName("sunrise") val sunrise: Long,
    @SerialName("sunset") val sunset: Long,
)

@Serializable
data class WeatherDescDto(
    @SerialName("description") val description: String = "",
    @SerialName("icon") val icon: String = "",
)
