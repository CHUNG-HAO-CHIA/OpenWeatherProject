package com.app.openweather

import android.app.Application
import com.app.openweather.core.data.di.dataModule
import com.app.openweather.core.domain.di.domainModule
import com.app.openweather.core.network.di.networkModule
import com.app.openweather.feature.city.di.cityModule
import com.app.openweather.feature.weather.di.weatherModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class WeatherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WeatherApplication)
            modules(
                networkModule,
                dataModule,
                domainModule,
                weatherModule,
                cityModule,
            )
        }
    }
}
