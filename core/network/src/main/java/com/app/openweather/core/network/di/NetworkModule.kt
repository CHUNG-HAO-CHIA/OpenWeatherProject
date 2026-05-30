package com.app.openweather.core.network.di

import com.app.openweather.core.network.BuildConfig
import com.app.openweather.core.network.api.NominatimApi
import com.app.openweather.core.network.api.WeatherApi
import com.app.openweather.core.network.interceptor.ApiKeyInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val OWM = "owm"
private const val NOMINATIM = "nominatim"

val networkModule = module {

    // Base client — timeouts only, no app-specific interceptors
    single {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .build()
    }

    // OWM client — injects API key automatically via interceptor
    single(named(OWM)) {
        get<OkHttpClient>().newBuilder()
            .addInterceptor(ApiKeyInterceptor(BuildConfig.API_KEY))
            .build()
    }

    // Nominatim client — attaches required User-Agent header per OSM policy
    single(named(NOMINATIM)) {
        get<OkHttpClient>().newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "OpenWeatherApp/1.0 (${BuildConfig.APPLICATION_ID})")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    single(named(OWM)) {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(get(named(OWM)))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single(named(NOMINATIM)) {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(get(named(NOMINATIM)))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<WeatherApi> { get<Retrofit>(named(OWM)).create(WeatherApi::class.java) }
    single<NominatimApi> { get<Retrofit>(named(NOMINATIM)).create(NominatimApi::class.java) }
}
