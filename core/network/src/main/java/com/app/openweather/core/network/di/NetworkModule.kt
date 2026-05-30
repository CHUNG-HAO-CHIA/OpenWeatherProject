package com.app.openweather.core.network.di

import com.app.openweather.core.network.BuildConfig
import com.app.openweather.core.network.api.NominatimApi
import com.app.openweather.core.network.api.WeatherApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val OWM = "owm"
private const val NOMINATIM = "nominatim"

val networkModule = module {
    single {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    single(named(OWM)) {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single(named(NOMINATIM)) {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<WeatherApi> { get<Retrofit>(named(OWM)).create(WeatherApi::class.java) }
    single<NominatimApi> { get<Retrofit>(named(NOMINATIM)).create(NominatimApi::class.java) }
}
