package com.app.openweather.core.domain.usecase

import com.app.openweather.core.common.coordKey
import com.app.openweather.core.domain.repository.CityRepository
import com.app.openweather.core.domain.model.SavedCity

class SearchCitiesUseCase(private val repository: CityRepository) {
    suspend operator fun invoke(query: String): List<SavedCity> {
        return repository.searchCities(query)
            .filter { it.country.isNotBlank() } // Must at least have a country
            .distinctBy { coordKey(it.lat, it.lon) }
    }
}
