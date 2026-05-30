package com.app.openweather.core.network.dto

import com.google.gson.annotations.SerializedName

data class CurrentWeatherDto(
    @SerializedName("name") val name: String,
    @SerializedName("main") val main: MainDto,
    @SerializedName("weather") val weather: List<WeatherDescDto>,
    @SerializedName("wind") val wind: WindDto,
    @SerializedName("sys") val sys: SysDto,
)

data class ForecastDto(
    @SerializedName("list") val list: List<DailyDto>,
)

data class DailyDto(
    @SerializedName("dt") val dt: Long,
    @SerializedName("temp") val temp: TempDto,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("weather") val weather: List<WeatherDescDto>,
    @SerializedName("speed") val speed: Double,
)

data class MainDto(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("humidity") val humidity: Int,
)

data class TempDto(
    @SerializedName("min") val min: Double,
    @SerializedName("max") val max: Double,
)

data class WeatherDescDto(
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String,
)

data class WindDto(
    @SerializedName("speed") val speed: Double,
)

data class SysDto(
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long,
)
