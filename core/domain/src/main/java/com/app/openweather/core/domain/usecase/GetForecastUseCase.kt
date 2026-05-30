package com.app.openweather.core.domain.usecase

import com.app.openweather.core.domain.repository.WeatherRepository

class GetForecastUseCase(private val repository: WeatherRepository) {
    operator fun invoke(lat: Double, lon: Double) = repository.getForecast(lat, lon)
}
