package com.app.openweather.core.domain.usecase

import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.repository.WeatherRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class GetWeatherForCitiesUseCase(private val repository: WeatherRepository) {
    suspend operator fun invoke(cities: List<SavedCity>): Map<String, CurrentWeather> = coroutineScope {
        cities.map { city ->
            async {
                val weatherResult = repository.getCurrentWeather(city.lat, city.lon).first { it !is Result.Loading }
                if (weatherResult is Result.Success) {
                    city.id to weatherResult.data
                } else null
            }
        }.awaitAll().filterNotNull().toMap()
    }
}
