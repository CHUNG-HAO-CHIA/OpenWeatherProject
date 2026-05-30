package com.app.openweather.core.domain.usecase

import com.app.openweather.core.domain.repository.CityRepository

class SearchCitiesUseCase(private val repository: CityRepository) {
    suspend operator fun invoke(query: String) = repository.searchCities(query)
}
