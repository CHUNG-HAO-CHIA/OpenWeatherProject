package com.app.openweather.core.network.api

import com.app.openweather.core.network.dto.CurrentWeatherDto
import com.app.openweather.core.network.dto.ForecastDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String,
    ): CurrentWeatherDto

    @GET("forecast/daily")
    suspend fun getWeeklyForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("cnt") count: Int = 7,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String,
    ): ForecastDto
}
