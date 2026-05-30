package com.app.openweather.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.openweather.core.data.local.dao.WeatherDao
import com.app.openweather.core.data.local.entity.CurrentWeatherEntity
import com.app.openweather.core.data.local.entity.DailyForecastEntity
import com.app.openweather.core.data.local.entity.HourlyForecastEntity

@Database(
    entities = [CurrentWeatherEntity::class, HourlyForecastEntity::class, DailyForecastEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
}
