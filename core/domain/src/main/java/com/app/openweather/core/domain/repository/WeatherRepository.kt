package com.app.openweather.core.domain.repository

import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.model.HourlyForecast
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun getCurrentWeather(lat: Double, lon: Double): Flow<Result<CurrentWeather>>
    fun getHourlyForecast(lat: Double, lon: Double): Flow<Result<List<HourlyForecast>>>
    fun getWeeklyForecast(lat: Double, lon: Double): Flow<Result<List<DailyForecast>>>
}
