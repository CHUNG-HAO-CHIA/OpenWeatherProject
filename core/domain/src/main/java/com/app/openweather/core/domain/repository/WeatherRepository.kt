package com.app.openweather.core.domain.repository

import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.Forecast
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun getCurrentWeather(lat: Double, lon: Double): Flow<Result<CurrentWeather>>
    fun getForecast(lat: Double, lon: Double): Flow<Result<Forecast>>
}
