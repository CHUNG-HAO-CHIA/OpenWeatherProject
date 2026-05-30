package com.app.openweather.core.data.repository

import com.app.openweather.core.common.Result
import com.app.openweather.core.data.local.dao.WeatherDao
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.model.Forecast
import com.app.openweather.core.domain.model.HourlyForecast
import com.app.openweather.core.domain.model.RawForecastItem
import com.app.openweather.core.domain.repository.WeatherRepository
import com.app.openweather.core.network.api.WeatherApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

class WeatherRepositoryImpl(
    private val api: WeatherApi,
    private val dao: WeatherDao,
) : WeatherRepository {

    override fun getCurrentWeather(lat: Double, lon: Double): Flow<Result<CurrentWeather>> = flow {
        val cityKey = cityKey(lat, lon)
        val cached = dao.observeCurrentWeather(cityKey).firstOrNull()
        if (cached != null) emit(Result.Success(cached.toDomain())) else emit(Result.Loading)

        try {
            val domain = api.getCurrentWeather(lat, lon).toDomain()
            dao.upsertCurrentWeather(domain.toEntity(cityKey))
            emit(Result.Success(domain))
        } catch (e: Exception) {
            if (cached == null) emit(Result.Error(e))
        }

        emitAll(dao.observeCurrentWeather(cityKey).map { entity ->
            if (entity != null) Result.Success(entity.toDomain())
            else Result.Error(Exception("No cached data"))
        })
    }

    override fun getForecast(lat: Double, lon: Double): Flow<Result<Forecast>> = flow {
        val cityKey = cityKey(lat, lon)
        val cachedHourly = dao.observeHourlyForecast(cityKey).firstOrNull()
        val cachedDaily = dao.observeDailyForecast(cityKey).firstOrNull()

        if (!cachedHourly.isNullOrEmpty() && !cachedDaily.isNullOrEmpty()) {
            emit(Result.Success(Forecast(
                hourly = cachedHourly.map { it.toDomain() },
                daily = cachedDaily.map { it.toDomain() },
            )))
        } else {
            emit(Result.Loading)
        }

        try {
            val allItems = api.getForecast(lat, lon).list
            val rawItems = allItems.map { it.toRawDomain() }
            
            val forecast = calculateForecast(rawItems)

            dao.deleteHourlyForecast(cityKey)
            dao.upsertHourlyForecasts(forecast.hourly.map { it.toEntity(cityKey) })

            dao.deleteDailyForecast(cityKey)
            dao.upsertDailyForecasts(forecast.daily.map { it.toEntity(cityKey) })
        } catch (e: Exception) {
            if (cachedHourly.isNullOrEmpty() || cachedDaily.isNullOrEmpty()) {
                emit(Result.Error(e))
            }
        }

        emitAll(
            dao.observeHourlyForecast(cityKey).combine(dao.observeDailyForecast(cityKey)) { hourly, daily ->
                if (hourly.isNotEmpty() && daily.isNotEmpty()) {
                    Result.Success(Forecast(
                        hourly = hourly.map { it.toDomain() },
                        daily = daily.map { it.toDomain() },
                    ))
                } else {
                    Result.Error(Exception("No cached forecast"))
                }
            }
        )
    }

    private fun cityKey(lat: Double, lon: Double) = String.format(java.util.Locale.US, "%.4f,%.4f", lat, lon)

    // visible for testing
    internal fun calculateForecast(rawItems: List<RawForecastItem>): Forecast {
        val hourly = rawItems.take(24).map { it.toHourly() }
        
        val daily = rawItems.groupBy { it.dtTxt.take(10) }.map { (_, dayItems) ->
            val representative = dayItems.closestToNoon()
            val tempMin = dayItems.minOf { it.tempMin }
            val tempMax = dayItems.maxOf { it.tempMax }
            
            DailyForecast(
                date = representative.dt,
                tempMin = tempMin,
                tempMax = tempMax,
                description = representative.description,
                iconCode = representative.iconCode,
                humidity = representative.humidity,
                windSpeed = representative.windSpeed,
                pop = representative.pop
            )
        }
        
        return Forecast(hourly, daily)
    }

    private fun List<RawForecastItem>.closestToNoon(): RawForecastItem {
        val noonSeconds = 12 * 3600
        return minByOrNull { item ->
            val timeOfDay = (item.dt % 86400).toInt()
            abs(timeOfDay - noonSeconds)
        } ?: first()
    }

    private fun RawForecastItem.toHourly() = HourlyForecast(
        dt = dt,
        temp = temp,
        feelsLike = feelsLike,
        humidity = humidity,
        windSpeed = windSpeed,
        pop = pop,
        description = description,
        iconCode = iconCode,
    )
}
