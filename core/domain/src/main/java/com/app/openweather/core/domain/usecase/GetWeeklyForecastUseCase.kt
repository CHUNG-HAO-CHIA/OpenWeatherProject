package com.app.openweather.core.domain.usecase

import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow

class GetWeeklyForecastUseCase(
    private val repository: WeatherRepository
) {
    operator fun invoke(lat: Double, lon: Double): Flow<Result<List<DailyForecast>>> =
        repository.getWeeklyForecast(lat, lon)
}
