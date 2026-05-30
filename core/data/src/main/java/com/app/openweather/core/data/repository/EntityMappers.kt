package com.app.openweather.core.data.repository

import com.app.openweather.core.data.local.entity.CurrentWeatherEntity
import com.app.openweather.core.data.local.entity.DailyForecastEntity
import com.app.openweather.core.data.local.entity.HourlyForecastEntity
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.model.HourlyForecast
import org.json.JSONObject

fun CurrentWeatherEntity.toDomain() = CurrentWeather(
    cityName = cityName,
    temperature = temperature,
    feelsLike = feelsLike,
    humidity = humidity,
    windSpeed = windSpeed,
    description = description,
    iconCode = iconCode,
    sunrise = sunrise,
    sunset = sunset,
    localizedNames = localizedNames.parseLocalizedNames(),
)

fun CurrentWeather.toEntity(cityKey: String) = CurrentWeatherEntity(
    cityKey = cityKey,
    cityName = cityName,
    temperature = temperature,
    feelsLike = feelsLike,
    humidity = humidity,
    windSpeed = windSpeed,
    description = description,
    iconCode = iconCode,
    sunrise = sunrise,
    sunset = sunset,
    localizedNames = localizedNames.toJsonString(),
)

private fun Map<String, String>.toJsonString(): String {
    val obj = JSONObject()
    forEach { (k, v) -> obj.put(k, v) }
    return obj.toString()
}

private fun String.parseLocalizedNames(): Map<String, String> {
    if (isEmpty()) return emptyMap()
    return try {
        val obj = JSONObject(this)
        buildMap { obj.keys().forEach { k -> put(k, obj.getString(k)) } }
    } catch (_: Exception) {
        emptyMap()
    }
}

fun HourlyForecastEntity.toDomain() = HourlyForecast(
    dt = dt,
    temp = temp,
    feelsLike = feelsLike,
    humidity = humidity,
    windSpeed = windSpeed,
    pop = pop,
    description = description,
    iconCode = iconCode,
)

fun HourlyForecast.toEntity(cityKey: String) = HourlyForecastEntity(
    id = "$cityKey-$dt",
    cityKey = cityKey,
    dt = dt,
    temp = temp,
    feelsLike = feelsLike,
    humidity = humidity,
    windSpeed = windSpeed,
    pop = pop,
    description = description,
    iconCode = iconCode,
)

fun DailyForecastEntity.toDomain() = DailyForecast(
    date = date,
    tempMin = tempMin,
    tempMax = tempMax,
    description = description,
    iconCode = iconCode,
    humidity = humidity,
    windSpeed = windSpeed,
    pop = pop,
)

fun DailyForecast.toEntity(cityKey: String) = DailyForecastEntity(
    id = "$cityKey-$date",
    cityKey = cityKey,
    date = date,
    tempMin = tempMin,
    tempMax = tempMax,
    description = description,
    iconCode = iconCode,
    humidity = humidity,
    windSpeed = windSpeed,
    pop = pop,
)
