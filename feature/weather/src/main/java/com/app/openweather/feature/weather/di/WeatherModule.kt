package com.app.openweather.feature.weather.di

import com.app.openweather.feature.weather.viewmodel.WeatherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val weatherModule = module {
    viewModel { WeatherViewModel(get()) }
}
