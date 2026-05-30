package com.app.openweather.core.network.dto

import com.google.gson.annotations.SerializedName

// data/2.5/weather
data class CurrentWeatherDto(
    @SerializedName("name") val name: String,
    @SerializedName("main") val main: MainDto,
    @SerializedName("weather") val weather: List<WeatherDescDto>,
    @SerializedName("wind") val wind: WindDto,
    @SerializedName("sys") val sys: SysDto,
)

// data/2.5/forecast  (every 3 h, up to 40 entries / 5 days)
data class ForecastResponseDto(
    @SerializedName("list") val list: List<ForecastItemDto>,
)

data class ForecastItemDto(
    @SerializedName("dt") val dt: Long,
    @SerializedName("main") val main: MainDto,
    @SerializedName("weather") val weather: List<WeatherDescDto>,
    @SerializedName("wind") val wind: WindDto,
    @SerializedName("pop") val pop: Double = 0.0,
    @SerializedName("dt_txt") val dtTxt: String,
)

data class MainDto(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("humidity") val humidity: Int,
)

data class WindDto(
    @SerializedName("speed") val speed: Double,
)

data class SysDto(
    @SerializedName("sunrise") val sunrise: Long,
    @SerializedName("sunset") val sunset: Long,
)

data class WeatherDescDto(
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String,
)
