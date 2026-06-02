package com.app.openweather.feature.weather.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.openweather.core.common.AppError
import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.model.HourlyForecast
import com.app.openweather.core.domain.usecase.WeatherUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

sealed class WeatherUiEvent {
    data class ShowSnackbar(val error: AppError) : WeatherUiEvent()
}

data class CurrentWeatherUiModel(
    val cityName: String,
    val temperature: String,
    val feelsLike: String,
    val humidity: String,
    val windSpeed: String,
    val description: String,
    val iconCode: String,
    val sunrise: String,
    val sunset: String,
)

data class HourlyForecastUiModel(
    val tempValue: Double,
    val tempLabel: String,
    val iconCode: String,
    val timeLabel: String,
    val dateLabel: String,
    val popLabel: String,
    val isNewDay: Boolean,
    val isFirst: Boolean,
)

data class DailyForecastUiModel(
    val dayLabel: String,
    val iconCode: String,
    val popLabel: String,
    val tempMinLabel: String,
    val tempMaxLabel: String,
)

data class WeatherUiState(
    val isLoading: Boolean = false,
    val currentWeather: CurrentWeatherUiModel? = null,
    val hourlyForecast: List<HourlyForecastUiModel> = emptyList(),
    val weeklyForecast: List<DailyForecastUiModel> = emptyList(),
    val pop: String = "—",
    val isOffline: Boolean = false,
    val lastUpdated: Long? = null,
    val timezoneOffsetSec: Int = 0,
    val error: AppError? = null,
)

class WeatherViewModel(
    private val useCases: WeatherUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _event = Channel<WeatherUiEvent>()
    val event = _event.receiveAsFlow()

    private var loadJob: Job? = null

    private fun timeFmt(tz: java.util.TimeZone) =
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = tz }
    private fun dayFmt(tz: java.util.TimeZone) =
        SimpleDateFormat("M/d E", Locale.getDefault()).apply { timeZone = tz }
    private fun dailyDayFmt(tz: java.util.TimeZone) =
        SimpleDateFormat("EEEE", Locale.getDefault()).apply { timeZone = tz }

    fun loadWeather(lat: Double, lon: Double) {
        loadJob?.cancel()
        // Start fresh for new location, but keep data if refreshing
        _uiState.update { it.copy(error = null) }
        
        loadJob = viewModelScope.launch {
            combine(
                useCases.getCurrentWeather(lat, lon),
                useCases.getForecast(lat, lon),
            ) { weatherResult, forecastResult ->
                Pair(weatherResult, forecastResult)
            }.collect { (weatherResult, forecastResult) ->
                _uiState.update { state ->
                    var next = state

                    // 先從 forecast 取最新 timezone（或沿用已存的）
                    val tzOffset = when (forecastResult) {
                        is Result.Success -> forecastResult.data.timezoneOffsetSec
                        is Result.Offline -> forecastResult.cachedData.timezoneOffsetSec
                        else -> state.timezoneOffsetSec
                    }
                    next = next.copy(timezoneOffsetSec = tzOffset)

                    next = when (weatherResult) {
                        is Result.Loading -> next.copy(isLoading = true)
                        is Result.Success -> {
                            next.copy(
                                isLoading = false,
                                isOffline = false,
                                currentWeather = mapCurrentWeather(weatherResult.data, tzOffset),
                            )
                        }
                        is Result.Offline -> {
                            next.copy(
                                isLoading = false,
                                isOffline = true,
                                lastUpdated = weatherResult.cachedAt,
                                currentWeather = mapCurrentWeather(weatherResult.cachedData, tzOffset),
                            )
                        }
                        is Result.Error -> {
                            if (next.currentWeather == null) {
                                // Hard failure (no data): show full screen error
                                next.copy(isLoading = false, error = weatherResult.error)
                            } else {
                                // Background failure: trigger snackbar and clear error in state to prevent UI jump
                                viewModelScope.launch { _event.send(WeatherUiEvent.ShowSnackbar(weatherResult.error)) }
                                next.copy(isLoading = false, error = null)
                            }
                        }
                    }

                    next = when (forecastResult) {
                        is Result.Success -> next.copy(
                            hourlyForecast = mapHourlyForecast(forecastResult.data.hourly, tzOffset),
                            weeklyForecast = mapDailyForecast(forecastResult.data.daily, tzOffset),
                            pop = "${((forecastResult.data.hourly.firstOrNull()?.pop ?: 0.0) * 100).roundToInt()}%",
                        )
                        is Result.Offline -> next.copy(
                            hourlyForecast = mapHourlyForecast(forecastResult.cachedData.hourly, tzOffset),
                            weeklyForecast = mapDailyForecast(forecastResult.cachedData.daily, tzOffset),
                            pop = "${((forecastResult.cachedData.hourly.firstOrNull()?.pop ?: 0.0) * 100).roundToInt()}%",
                        )
                        is Result.Error -> {
                            if (next.currentWeather == null) {
                                next.copy(error = forecastResult.error)
                            } else {
                                viewModelScope.launch { _event.send(WeatherUiEvent.ShowSnackbar(forecastResult.error)) }
                                next.copy(error = null)
                            }
                        }
                        is Result.Loading -> next
                    }

                    next
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun mapCurrentWeather(domain: CurrentWeather, timezoneOffsetSec: Int): CurrentWeatherUiModel {
        val cityTz = java.util.TimeZone.getTimeZone("GMT").also {
            it.rawOffset = timezoneOffsetSec * 1000
        }
        return CurrentWeatherUiModel(
            cityName = domain.cityName,
            temperature = "${domain.temperature.roundToInt()}°C",
            feelsLike = "${domain.feelsLike.roundToInt()}°",
            humidity = "${domain.humidity}%",
            windSpeed = "${(domain.windSpeed * 3.6).roundToInt()} km/h",
            description = domain.description.replaceFirstChar { it.uppercase() },
            iconCode = domain.iconCode,
            sunrise = timeFmt(cityTz).format(Date(domain.sunrise * 1000)),
            sunset = timeFmt(cityTz).format(Date(domain.sunset * 1000)),
        )
    }

    private fun mapHourlyForecast(
        domainList: List<HourlyForecast>,
        timezoneOffsetSec: Int,
    ): List<HourlyForecastUiModel> {
        if (domainList.isEmpty()) return emptyList()
        val tz = java.util.TimeZone.getTimeZone("GMT").also {
            it.rawOffset = timezoneOffsetSec * 1000
        }
        val fmt = timeFmt(tz)
        val dFmt = dayFmt(tz)
        val cal = Calendar.getInstance(tz)
        var prevDay = -1
        return domainList.mapIndexed { index, item ->
            cal.timeInMillis = item.dt * 1000
            val curDay = cal.get(Calendar.DAY_OF_YEAR)
            val isNewDay = if (index == 0) true else curDay != prevDay
            prevDay = curDay
            val date = Date(item.dt * 1000)
            HourlyForecastUiModel(
                tempValue = item.temp,
                tempLabel = "${item.temp.roundToInt()}°",
                iconCode = item.iconCode,
                timeLabel = fmt.format(date),
                dateLabel = dFmt.format(date),
                popLabel = "${(item.pop * 100).roundToInt()}%",
                isNewDay = isNewDay,
                isFirst = index == 0,
            )
        }
    }

    private fun mapDailyForecast(
        domainList: List<DailyForecast>,
        timezoneOffsetSec: Int,
    ): List<DailyForecastUiModel> {
        val tz = java.util.TimeZone.getTimeZone("GMT").also {
            it.rawOffset = timezoneOffsetSec * 1000
        }
        val fmt = dailyDayFmt(tz)
        return domainList.map { item ->
            DailyForecastUiModel(
                dayLabel = fmt.format(Date(item.date * 1000)),
                iconCode = item.iconCode,
                popLabel = "${(item.pop * 100).roundToInt()}%",
                tempMinLabel = "${item.tempMin.roundToInt()}°",
                tempMaxLabel = "${item.tempMax.roundToInt()}°",
            )
        }
    }
}
