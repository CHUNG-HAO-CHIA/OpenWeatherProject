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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.abs

import com.app.openweather.core.common.coordKey
import com.app.openweather.core.network.api.NominatimApi
import com.app.openweather.core.network.dto.NominatimDto

class WeatherRepositoryImpl(
    private val api: WeatherApi,
    private val dao: WeatherDao,
    private val nominatimApi: NominatimApi,
) : WeatherRepository {

    override fun getCurrentWeather(lat: Double, lon: Double): Flow<Result<CurrentWeather>> = flow {
        val cityKey = cityKey(lat, lon)

        // Initial state: try to get from DB first
        val initialCached = dao.observeCurrentWeather(cityKey).map { it?.toDomain() }.firstOrNull()
        if (initialCached != null) {
            emit(Result.Success(initialCached))
        } else {
            emit(Result.Loading)
        }

        // Try to refresh from API
        try {
            val weatherDomain = api.getCurrentWeather(lat, lon).toDomain()
            val nominatimDto = try {
                nominatimApi.reverse(lat, lon)
            } catch (_: Exception) {
                null
            }
            
            val domain = weatherDomain.copy(
                cityName = nominatimDto?.resolveCityName() ?: weatherDomain.cityName,
                localizedNames = nominatimDto?.namedetails ?: emptyMap()
            )
            dao.upsertCurrentWeather(domain.toEntity(cityKey))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Only emit error if we don't have cached data
            if (initialCached == null) {
                emit(Result.Error(e))
            }
        }

        // Observe continuous updates from DB
        dao.observeCurrentWeather(cityKey).collect { entity ->
            if (entity != null) {
                emit(Result.Success(entity.toDomain()))
            }
        }
    }.flowOn(Dispatchers.IO)

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
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (cachedHourly.isNullOrEmpty() || cachedDaily.isNullOrEmpty()) {
                emit(Result.Error(e))
            }
        }

        dao.observeHourlyForecast(cityKey).combine(dao.observeDailyForecast(cityKey)) { hourly, daily ->
            if (hourly.isNotEmpty() && daily.isNotEmpty()) {
                Forecast(
                    hourly = hourly.map { it.toDomain() },
                    daily = daily.map { it.toDomain() },
                )
            } else null
        }.collect { forecast ->
            if (forecast != null) {
                emit(Result.Success(forecast))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun cityKey(lat: Double, lon: Double) = coordKey(lat, lon)

    // visible for testing
    internal suspend fun calculateForecast(rawItems: List<RawForecastItem>): Forecast =
        withContext(Dispatchers.Default) {
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
        Forecast(hourly, daily)
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
