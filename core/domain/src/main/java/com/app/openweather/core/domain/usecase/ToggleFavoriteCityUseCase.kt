package com.app.openweather.core.domain.usecase

import com.app.openweather.core.domain.repository.CityRepository

const val MAX_FAVORITES = 3

class ToggleFavoriteCityUseCase(private val repository: CityRepository) {
    suspend operator fun invoke(cityId: String) = repository.toggleFavorite(cityId)
}
