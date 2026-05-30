package com.app.openweather.core.domain.usecase

import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.repository.CityRepository

class SaveCityUseCase(private val repository: CityRepository) {
    suspend operator fun invoke(city: SavedCity) = repository.saveCity(city)
}
