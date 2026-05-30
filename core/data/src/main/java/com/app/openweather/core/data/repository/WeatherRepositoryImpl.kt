package com.app.openweather.core.data.repository

import com.app.openweather.core.common.Result
import com.app.openweather.core.data.local.dao.WeatherDao
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.Forecast
import com.app.openweather.core.domain.repository.WeatherRepository
import com.app.openweather.core.network.api.WeatherApi
import com.app.openweather.core.network.dto.ForecastItemDto
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

            val hourlyDomains = allItems.take(24).map { it.toHourlyDomain() }
            dao.deleteHourlyForecast(cityKey)
            dao.upsertHourlyForecasts(hourlyDomains.map { it.toEntity(cityKey) })

            val dailyDomains = allItems.groupByDay().map { (_, dayItems) ->
                val representative = dayItems.closestToNoon()
                val tempMin = dayItems.minOf { it.main.tempMin }
                val tempMax = dayItems.maxOf { it.main.tempMax }
                representative.toDailyDomain(tempMin, tempMax)
            }
            dao.deleteDailyForecast(cityKey)
            dao.upsertDailyForecasts(dailyDomains.map { it.toEntity(cityKey) })
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
}

private fun List<ForecastItemDto>.groupByDay(): Map<String, List<ForecastItemDto>> =
    groupBy { it.dtTxt.take(10) }

private fun List<ForecastItemDto>.closestToNoon(): ForecastItemDto {
    val noonSeconds = 12 * 3600
    return minByOrNull { item ->
        val timeOfDay = (item.dt % 86400).toInt()
        abs(timeOfDay - noonSeconds)
    } ?: first()
}
