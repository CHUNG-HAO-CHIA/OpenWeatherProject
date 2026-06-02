package com.app.openweather.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.openweather.core.common.Result
import com.app.openweather.core.data.local.AppDatabase
import com.app.openweather.core.domain.usecase.WeatherUseCases
import kotlinx.coroutines.flow.first

class WeatherWidgetWorker(
    private val context: Context,
    params: WorkerParameters,
    private val db: AppDatabase,
    private val weatherUseCases: WeatherUseCases,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            val cities = db.cityDao().getAllCities()
            val firstCity = cities.firstOrNull() ?: return androidx.work.ListenableWorker.Result.success()

            val result = weatherUseCases.getCurrentWeather(firstCity.lat, firstCity.lon)
                .first { it !is Result.Loading }

            if (result is Result.Error || result is Result.Offline) {
                return if (runAttemptCount < 3) androidx.work.ListenableWorker.Result.retry()
                else androidx.work.ListenableWorker.Result.failure()
            }

            WeatherWidget().updateAll(context)

            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) androidx.work.ListenableWorker.Result.retry() else androidx.work.ListenableWorker.Result.failure()
        }
    }
}
