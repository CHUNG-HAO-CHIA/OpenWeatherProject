package com.app.openweather.core.data.repository

import com.app.openweather.core.common.Result
import com.app.openweather.core.data.local.dao.WeatherDao
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.model.HourlyForecast
import com.app.openweather.core.domain.repository.WeatherRepository
import com.app.openweather.core.network.BuildConfig
import com.app.openweather.core.network.api.WeatherApi
import com.app.openweather.core.network.dto.ForecastItemDto
import kotlinx.coroutines.flow.Flow
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
            val domain = api.getCurrentWeather(lat, lon, apiKey = BuildConfig.API_KEY).toDomain()
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

    override fun getHourlyForecast(lat: Double, lon: Double): Flow<Result<List<HourlyForecast>>> = flow {
        val cityKey = cityKey(lat, lon)
        val cached = dao.observeHourlyForecast(cityKey).firstOrNull()
        if (!cached.isNullOrEmpty()) emit(Result.Success(cached.map { it.toDomain() })) else emit(Result.Loading)

        try {
            // forecast gives up to 40 entries every 3 h → show first 24 (= 72 h)
            val items = api.getForecast(lat, lon, apiKey = BuildConfig.API_KEY).list.take(24)
            val domains = items.map { it.toHourlyDomain() }
            dao.deleteHourlyForecast(cityKey)
            dao.upsertHourlyForecasts(domains.map { it.toEntity(cityKey) })
            emit(Result.Success(domains))
        } catch (e: Exception) {
            if (cached.isNullOrEmpty()) emit(Result.Error(e))
        }

        emitAll(dao.observeHourlyForecast(cityKey).map { entities ->
            if (entities.isNotEmpty()) Result.Success(entities.map { it.toDomain() })
            else Result.Error(Exception("No cached hourly data"))
        })
    }

    override fun getWeeklyForecast(lat: Double, lon: Double): Flow<Result<List<DailyForecast>>> = flow {
        val cityKey = cityKey(lat, lon)
        val cached = dao.observeDailyForecast(cityKey).firstOrNull()
        if (!cached.isNullOrEmpty()) emit(Result.Success(cached.map { it.toDomain() })) else emit(Result.Loading)

        try {
            val allItems = api.getForecast(lat, lon, apiKey = BuildConfig.API_KEY).list
            val domains = allItems.groupByDay().map { (_, dayItems) ->
                val representative = dayItems.closestToNoon()
                val tempMin = dayItems.minOf { it.main.tempMin }
                val tempMax = dayItems.maxOf { it.main.tempMax }
                representative.toDailyDomain(tempMin, tempMax)
            }
            dao.deleteDailyForecast(cityKey)
            dao.upsertDailyForecasts(domains.map { it.toEntity(cityKey) })
            emit(Result.Success(domains))
        } catch (e: Exception) {
            if (cached.isNullOrEmpty()) emit(Result.Error(e))
        }

        emitAll(dao.observeDailyForecast(cityKey).map { entities ->
            if (entities.isNotEmpty()) Result.Success(entities.map { it.toDomain() })
            else Result.Error(Exception("No cached forecast"))
        })
    }

    private fun cityKey(lat: Double, lon: Double) = "$lat,$lon"
}

// dt_txt = "2024-01-01 12:00:00" → date part is first 10 chars
private fun List<ForecastItemDto>.groupByDay(): Map<String, List<ForecastItemDto>> =
    groupBy { it.dtTxt.take(10) }

// Pick the entry whose time part is closest to 12:00
private fun List<ForecastItemDto>.closestToNoon(): ForecastItemDto {
    val noonSeconds = 12 * 3600
    return minByOrNull { item ->
        val timeOfDay = (item.dt % 86400).toInt()
        abs(timeOfDay - noonSeconds)
    } ?: first()
}
