package com.app.openweather.core.data.repository

import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.repository.WeatherRepository
import com.app.openweather.core.network.BuildConfig
import com.app.openweather.core.network.api.WeatherApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WeatherRepositoryImpl(
    private val api: WeatherApi,
) : WeatherRepository {

    override fun getCurrentWeather(lat: Double, lon: Double): Flow<Result<CurrentWeather>> = flow {
        emit(Result.Loading)
        try {
            val dto = api.getCurrentWeather(lat, lon, apiKey = BuildConfig.API_KEY)
            emit(Result.Success(dto.toDomain()))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getWeeklyForecast(lat: Double, lon: Double): Flow<Result<List<DailyForecast>>> = flow {
        emit(Result.Loading)
        try {
            val dto = api.getWeeklyForecast(lat, lon, apiKey = BuildConfig.API_KEY)
            emit(Result.Success(dto.list.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }
}
