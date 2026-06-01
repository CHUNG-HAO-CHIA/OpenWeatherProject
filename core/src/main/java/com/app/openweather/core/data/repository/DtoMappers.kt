package com.app.openweather.core.data.repository

import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.RawForecastItem
import com.app.openweather.core.network.dto.CurrentWeatherDto
import com.app.openweather.core.network.dto.ForecastItemDto
import com.app.openweather.core.network.dto.NominatimDto

/**
 * 統一的地點名稱解析優先序：
 * address.city → town → village → county → state → displayName 第一段 → displayName 完整
 */
fun NominatimDto.resolveCityName(): String {
    val addr = address
    return when {
        addr?.city != null -> "${addr.city}"
        addr?.town != null -> "${addr.town}"
        addr?.village != null -> "${addr.village}"
        addr?.county != null -> "${addr.county}"
        addr?.state != null -> "${addr.state}"
        else -> displayName
    }
}

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
