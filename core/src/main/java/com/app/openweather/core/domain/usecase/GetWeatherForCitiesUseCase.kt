package com.app.openweather.core.domain.usecase

import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GetWeatherForCitiesUseCase(private val repository: WeatherRepository) {
    suspend operator fun invoke(cities: List<SavedCity>): Map<String, CurrentWeather> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                cities.map { city ->
                    async {
                        val result = repository.getCurrentWeather(city.lat, city.lon)
                            .first { it !is Result.Loading }
                        if (result is Result.Success) city.id to result.data else null
                    }
                }.awaitAll().filterNotNull().toMap()
            }
        }
}
