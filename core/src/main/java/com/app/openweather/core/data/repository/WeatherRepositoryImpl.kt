package com.app.openweather.core.data.repository

import com.app.openweather.core.common.Result
import com.app.openweather.core.common.toAppError
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

private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour

class WeatherRepositoryImpl(
    private val api: WeatherApi,
    private val dao: WeatherDao,
    private val nominatimApi: NominatimApi,
) : WeatherRepository {

    override fun getCurrentWeather(lat: Double, lon: Double): Flow<Result<CurrentWeather>> = flow {
        val cityKey = cityKey(lat, lon)

        // Initial state: try to get from DB first
        val initialCachedEntity = dao.observeCurrentWeather(cityKey).firstOrNull()
        val initialCached = initialCachedEntity?.toDomain()
        if (initialCached != null) {
            emit(Result.Success(initialCached))
        } else {
            emit(Result.Loading)
        }

        val now = System.currentTimeMillis()
        val isFresh = initialCachedEntity != null && (now - initialCachedEntity.updatedAt < CACHE_TTL_MS)

        if (!isFresh) {
            // Try to refresh from API
            try {
                val weatherDomain = api.getCurrentWeather(lat, lon).toDomain()
                val nominatimDto = if (initialCached == null || initialCached.cityName.isBlank()) {
                    try {
                        nominatimApi.reverse(lat, lon)
                    } catch (_: Exception) {
                        null
                    }
                } else null
                
                val domain = weatherDomain.copy(
                    cityName = nominatimDto?.resolveCityName() ?: initialCached?.cityName ?: weatherDomain.cityName,
                    localizedNames = nominatimDto?.namedetails ?: initialCached?.localizedNames ?: emptyMap()
                )
                dao.upsertCurrentWeather(domain.toEntity(cityKey))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (initialCached == null) {
                    emit(Result.Error(e.toAppError()))
                } else {
                    val cachedAt = dao.getWeatherUpdatedAt(cityKey) ?: System.currentTimeMillis()
                    emit(Result.Offline(initialCached, cachedAt))
                }
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

        val now = System.currentTimeMillis()
        val updatedAt = cachedHourly?.firstOrNull()?.updatedAt ?: 0L
        val isFresh = cachedHourly?.isNotEmpty() == true && cachedDaily?.isNotEmpty() == true && (now - updatedAt < CACHE_TTL_MS)

        if (!isFresh) {
            try {
                val response = api.getForecast(lat, lon)
                val timezoneOffsetSec = response.city.timezone
                val rawItems = response.list.map { it.toRawDomain() }
                val forecast = calculateForecast(rawItems, timezoneOffsetSec)

                dao.deleteHourlyForecast(cityKey)
                dao.upsertHourlyForecasts(forecast.hourly.map { it.toEntity(cityKey) })

                dao.deleteDailyForecast(cityKey)
                dao.upsertDailyForecasts(forecast.daily.map { it.toEntity(cityKey) })
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (cachedHourly.isNullOrEmpty() || cachedDaily.isNullOrEmpty()) {
                    emit(Result.Error(e.toAppError()))
                } else {
                    val cachedAt = cachedHourly.firstOrNull()?.updatedAt ?: System.currentTimeMillis()
                    emit(Result.Offline(Forecast(
                        hourly = cachedHourly.map { it.toDomain() },
                        daily = cachedDaily.map { it.toDomain() },
                    ), cachedAt))
                }
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
    internal suspend fun calculateForecast(
        rawItems: List<RawForecastItem>,
        timezoneOffsetSec: Int = 0,
    ): Forecast = withContext(Dispatchers.Default) {
        val hourly = rawItems.take(24).map { it.toHourly() }

        // Group by local date using the city's timezone offset
        val daily = rawItems.groupBy { item ->
            val localMs = item.dt * 1000L + timezoneOffsetSec * 1000L
            localDateKey(localMs)
        }.mapNotNull { (_, dayItems) ->
            val representative = dayItems.closestToLocalNoon(timezoneOffsetSec) ?: return@mapNotNull null
            DailyForecast(
                date = representative.dt,
                tempMin = dayItems.minOf { it.tempMin },
                tempMax = dayItems.maxOf { it.tempMax },
                description = representative.description,
                iconCode = representative.iconCode,
                humidity = representative.humidity,
                windSpeed = representative.windSpeed,
                pop = representative.pop,
            )
        }
        Forecast(hourly, daily, timezoneOffsetSec)
    }

    private fun localDateKey(localMs: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return sdf.format(java.util.Date(localMs))
    }

    private fun List<RawForecastItem>.closestToLocalNoon(timezoneOffsetSec: Int): RawForecastItem? {
        val noonSeconds = 12 * 3600
        return minByOrNull { item ->
            val localTimeOfDay = ((item.dt + timezoneOffsetSec) % 86400).toInt()
            abs(localTimeOfDay - noonSeconds)
        }
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
