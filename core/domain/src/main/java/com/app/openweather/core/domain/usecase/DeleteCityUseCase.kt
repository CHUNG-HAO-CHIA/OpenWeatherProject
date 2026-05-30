package com.app.openweather.core.domain.usecase

import com.app.openweather.core.domain.repository.CityRepository

class DeleteCityUseCase(private val repository: CityRepository) {
    suspend operator fun invoke(cityId: String) = repository.deleteCity(cityId)
}
