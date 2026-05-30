package com.app.openweather.core.data.repository

import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.RawForecastItem
import com.app.openweather.core.network.dto.CurrentWeatherDto
import com.app.openweather.core.network.dto.ForecastItemDto

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

fun ForecastItemDto.toRawDomain() = RawForecastItem(
    dt = dt,
    dtTxt = dtTxt,
    temp = main.temp,
    tempMin = main.tempMin,
    tempMax = main.tempMax,
    feelsLike = main.feelsLike,
    humidity = main.humidity,
    windSpeed = wind.speed,
    pop = pop,
    description = weather.firstOrNull()?.description.orEmpty(),
    iconCode = weather.firstOrNull()?.icon.orEmpty(),
)
