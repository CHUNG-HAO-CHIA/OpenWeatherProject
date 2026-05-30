package com.app.openweather.core.domain.usecase

data class CityUseCases(
    val getSavedCities: GetSavedCitiesUseCase,
    val searchCities: SearchCitiesUseCase,
    val saveCity: SaveCityUseCase,
    val toggleFavorite: ToggleFavoriteCityUseCase,
    val deleteCity: DeleteCityUseCase,
    val reverseGeocode: ReverseGeocodeUseCase,
)