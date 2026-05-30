package com.app.openweather.core.domain.di

import com.app.openweather.core.domain.usecase.GetCurrentWeatherUseCase
import com.app.openweather.core.domain.usecase.GetWeeklyForecastUseCase
import org.koin.dsl.module

val domainModule = module {
    factory { GetCurrentWeatherUseCase(get()) }
    factory { GetWeeklyForecastUseCase(get()) }
}
