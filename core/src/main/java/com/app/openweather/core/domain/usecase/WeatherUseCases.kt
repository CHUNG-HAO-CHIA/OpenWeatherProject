package com.app.openweather.core.domain.usecase

data class WeatherUseCases(
    val getCurrentWeather: GetCurrentWeatherUseCase,
    val getForecast: GetForecastUseCase,
    val getWeatherForCities: GetWeatherForCitiesUseCase,
)