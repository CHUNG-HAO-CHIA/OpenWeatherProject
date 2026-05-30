package com.app.openweather.feature.city.di

import com.app.openweather.feature.city.viewmodel.CityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val cityModule = module {
    viewModel { CityViewModel() }
}
