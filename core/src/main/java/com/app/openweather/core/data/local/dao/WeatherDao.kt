package com.app.openweather.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.openweather.core.data.local.entity.CurrentWeatherEntity
import com.app.openweather.core.data.local.entity.DailyForecastEntity
import com.app.openweather.core.data.local.entity.HourlyForecastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    // --- Current weather ---

    @Query("SELECT * FROM current_weather WHERE cityKey = :cityKey")
    fun observeCurrentWeather(cityKey: String): Flow<CurrentWeatherEntity?>

    @Query("SELECT * FROM current_weather WHERE cityKey = :cityKey")
    suspend fun getCurrentWeather(cityKey: String): CurrentWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCurrentWeather(entity: CurrentWeatherEntity)

    @Query("SELECT updatedAt FROM current_weather WHERE cityKey = :cityKey")
    suspend fun getWeatherUpdatedAt(cityKey: String): Long?

    // --- Hourly forecast ---

    @Query("SELECT * FROM hourly_forecast WHERE cityKey = :cityKey ORDER BY dt ASC")
    fun observeHourlyForecast(cityKey: String): Flow<List<HourlyForecastEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHourlyForecasts(entities: List<HourlyForecastEntity>)

    @Query("DELETE FROM hourly_forecast WHERE cityKey = :cityKey")
    suspend fun deleteHourlyForecast(cityKey: String)

    // --- Daily forecast ---

    @Query("SELECT * FROM daily_forecast WHERE cityKey = :cityKey ORDER BY date ASC")
    fun observeDailyForecast(cityKey: String): Flow<List<DailyForecastEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyForecasts(entities: List<DailyForecastEntity>)

    @Query("DELETE FROM daily_forecast WHERE cityKey = :cityKey")
    suspend fun deleteDailyForecast(cityKey: String)
}
