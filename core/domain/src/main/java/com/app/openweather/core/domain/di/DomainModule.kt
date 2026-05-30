package com.app.openweather.core.domain.di

import com.app.openweather.core.domain.usecase.DeleteCityUseCase
import com.app.openweather.core.domain.usecase.GetCurrentWeatherUseCase
import com.app.openweather.core.domain.usecase.GetHourlyForecastUseCase
import com.app.openweather.core.domain.usecase.GetSavedCitiesUseCase
import com.app.openweather.core.domain.usecase.GetWeeklyForecastUseCase
import com.app.openweather.core.domain.usecase.SaveCityUseCase
import com.app.openweather.core.domain.usecase.SearchCitiesUseCase
import com.app.openweather.core.domain.usecase.ToggleFavoriteCityUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetCurrentWeatherUseCase(get()) }
    factory { GetHourlyForecastUseCase(get()) }
    factory { GetWeeklyForecastUseCase(get()) }
    factory { GetSavedCitiesUseCase(get()) }
    factory { SearchCitiesUseCase(get()) }
    factory { SaveCityUseCase(get()) }
    factory { ToggleFavoriteCityUseCase(get()) }
    factory { DeleteCityUseCase(get()) }
}
