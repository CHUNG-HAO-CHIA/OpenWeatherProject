package com.app.openweather.core.network.api

import com.app.openweather.core.network.dto.CurrentWeatherDto
import com.app.openweather.core.network.dto.ForecastResponseDto
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

    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String,
    ): ForecastResponseDto
}
