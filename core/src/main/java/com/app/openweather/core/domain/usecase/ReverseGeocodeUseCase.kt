package com.app.openweather.core.domain.usecase

import com.app.openweather.core.domain.repository.CityRepository

class ReverseGeocodeUseCase(private val repository: CityRepository) {
    suspend operator fun invoke(lat: Double, lon: Double) = repository.reverseGeocode(lat, lon)
}
