package com.app.openweather.core.domain.di

import com.app.openweather.core.domain.usecase.*
import org.koin.dsl.module

val domainModule = module {
    factory { GetCurrentWeatherUseCase(get()) }
    factory { GetForecastUseCase(get()) }
    factory { GetSavedCitiesUseCase(get()) }
    factory { SearchCitiesUseCase(get()) }
    factory { SaveCityUseCase(get()) }
    factory { ToggleFavoriteCityUseCase(get()) }
    factory { DeleteCityUseCase(get()) }
    factory { ReverseGeocodeUseCase(get()) }
    factory { GetWeatherForCitiesUseCase(get()) }

    factory { CityUseCases(get(), get(), get(), get(), get(), get()) }
    factory { WeatherUseCases(get(), get(), get()) }
}
