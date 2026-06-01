package com.app.openweather.core.domain.usecase

import com.app.openweather.core.domain.repository.CityRepository

class GetSavedCitiesUseCase(private val repository: CityRepository) {
    operator fun invoke() = repository.getSavedCities()
}
