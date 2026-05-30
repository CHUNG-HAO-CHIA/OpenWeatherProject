package com.app.openweather.core.data.repository

import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.model.HourlyForecast
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

fun ForecastItemDto.toHourlyDomain() = HourlyForecast(
    dt = dt,
    temp = main.temp,
    feelsLike = main.feelsLike,
    humidity = main.humidity,
    windSpeed = wind.speed,
    pop = pop,
    description = weather.firstOrNull()?.description.orEmpty(),
    iconCode = weather.firstOrNull()?.icon.orEmpty(),
)

// Each ForecastItemDto represents a 3h window; daily min/max are aggregated in the repository.
fun ForecastItemDto.toDailyDomain(tempMin: Double, tempMax: Double) = DailyForecast(
    date = dt,
    tempMin = tempMin,
    tempMax = tempMax,
    description = weather.firstOrNull()?.description.orEmpty(),
    iconCode = weather.firstOrNull()?.icon.orEmpty(),
    humidity = main.humidity,
    windSpeed = wind.speed,
    pop = pop,
)
