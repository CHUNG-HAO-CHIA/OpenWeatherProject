package com.app.openweather.feature.map.di

import com.app.openweather.feature.map.viewmodel.MapViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mapModule = module {
    viewModel { MapViewModel(get(), get()) }
}
