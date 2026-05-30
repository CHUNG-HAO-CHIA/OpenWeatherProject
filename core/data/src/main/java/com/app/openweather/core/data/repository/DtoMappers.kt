package com.app.openweather.core.data.repository

import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.network.dto.CurrentWeatherDto
import com.app.openweather.core.network.dto.DailyDto

fun CurrentWeatherDto.toDomain() = CurrentWeather(
    cityName = name,
    temperature = main.temp,
    feelsLike = main.feelsLike,
    humidity = main.humidity,
    windSpeed = wind.speed,
    description = weather.firstOrNull()?.description.orEmpty(),
    iconCode = weather.firstOrNull()?.icon.orEmpty(),
    sunrise = sys.sunrise,
    sunset = sys.sunset,
)

fun DailyDto.toDomain() = DailyForecast(
    date = dt,
    tempMin = temp.min,
    tempMax = temp.max,
    description = weather.firstOrNull()?.description.orEmpty(),
    iconCode = weather.firstOrNull()?.icon.orEmpty(),
    humidity = humidity,
    windSpeed = speed,
)
