package com.app.openweather.core.data.di

import com.app.openweather.core.data.repository.WeatherRepositoryImpl
import com.app.openweather.core.domain.repository.WeatherRepository
import org.koin.dsl.module

val dataModule = module {
    single<WeatherRepository> { WeatherRepositoryImpl(get()) }
}
