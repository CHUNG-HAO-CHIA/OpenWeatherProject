package com.app.openweather.core.data.di

import androidx.room.Room
import com.app.openweather.core.data.local.AppDatabase
import com.app.openweather.core.data.repository.WeatherRepositoryImpl
import com.app.openweather.core.domain.repository.WeatherRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "weather_db",
        ).fallbackToDestructiveMigration().build()
    }

    single { get<AppDatabase>().weatherDao() }

    single<WeatherRepository> { WeatherRepositoryImpl(get(), get()) }
}
