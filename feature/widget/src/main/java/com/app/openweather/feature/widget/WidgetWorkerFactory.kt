package com.app.openweather.feature.widget

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.app.openweather.core.data.local.AppDatabase
import com.app.openweather.core.domain.usecase.WeatherUseCases
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WidgetWorkerFactory : WorkerFactory(), KoinComponent {

    private val db: AppDatabase by inject()
    private val weatherUseCases: WeatherUseCases by inject()

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        return when (workerClassName) {
            WeatherWidgetWorker::class.java.name ->
                WeatherWidgetWorker(appContext, workerParameters, db, weatherUseCases)
            else -> null
        }
    }
}
