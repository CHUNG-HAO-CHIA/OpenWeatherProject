package com.app.openweather.feature.weather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.model.HourlyForecast
import com.app.openweather.core.domain.usecase.WeatherUseCases
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class CurrentWeatherUiModel(
    val cityName: String,
    val temperature: String,
    val feelsLike: String,
    val humidity: String,
    val windSpeed: String,
    val description: String,
    val iconUrl: String,
)

data class HourlyForecastUiModel(
    val tempValue: Double,
    val tempLabel: String,
    val iconUrl: String,
    val timeLabel: String,
    val dateLabel: String,
    val isNewDay: Boolean,
)

data class DailyForecastUiModel(
    val dayLabel: String,
    val iconUrl: String,
    val popLabel: String,
    val tempMinLabel: String,
    val tempMaxLabel: String,
)

data class WeatherUiState(
    val isLoading: Boolean = false,
    val currentWeather: CurrentWeatherUiModel? = null,
    val hourlyForecast: List<HourlyForecastUiModel> = emptyList(),
    val weeklyForecast: List<DailyForecastUiModel> = emptyList(),
    val error: String? = null,
)

class WeatherViewModel(
    private val useCases: WeatherUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFmt  = SimpleDateFormat("M/d E", Locale.getDefault())
    private val dailyDayFmt = SimpleDateFormat("EEEE", Locale.getDefault())

    fun loadWeather(lat: Double, lon: Double, overrideCityName: String? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                useCases.getCurrentWeather(lat, lon),
                useCases.getForecast(lat, lon),
            ) { weatherResult, forecastResult ->
                Pair(weatherResult, forecastResult)
            }.collect { (weatherResult, forecastResult) ->
                _uiState.update { state ->
                    var next = state

                    next = when (weatherResult) {
                        is Result.Loading -> next.copy(isLoading = true, error = null)
                        is Result.Success -> next.copy(isLoading = false, currentWeather = mapCurrentWeather(weatherResult.data, overrideCityName))
                        is Result.Error -> next.copy(isLoading = false, error = weatherResult.exception.message)
                    }

                    next = when (forecastResult) {
                        is Result.Success -> next.copy(
                            hourlyForecast = mapHourlyForecast(forecastResult.data.hourly),
                            weeklyForecast = mapDailyForecast(forecastResult.data.daily),
                        )
                        is Result.Error -> next.copy(error = forecastResult.exception.message)
                        is Result.Loading -> next
                    }

                    next
                }
            }
        }
    }

    private fun resolveLocalizedName(domain: CurrentWeather): String {
        val locale = Locale.getDefault()
        val lang = locale.language
        val country = locale.country
        val names = domain.localizedNames
        return names["name:$lang-$country"]
            ?: names["name:$lang"]
            ?: names["name"]
            ?: domain.cityName
    }

    private fun mapCurrentWeather(domain: CurrentWeather, overrideCityName: String? = null): CurrentWeatherUiModel {
        return CurrentWeatherUiModel(
            cityName = overrideCityName ?: resolveLocalizedName(domain),
            temperature = "${domain.temperature.roundToInt()}°C",
            feelsLike = "${domain.feelsLike.roundToInt()}°",
            humidity = "${domain.humidity}%",
            windSpeed = "${(domain.windSpeed * 3.6).roundToInt()} km/h",
            description = domain.description.replaceFirstChar { it.uppercase() },
            iconUrl = "https://openweathermap.org/img/wn/${domain.iconCode}@2x.png"
        )
    }

    private fun mapHourlyForecast(domainList: List<HourlyForecast>): List<HourlyForecastUiModel> {
        if (domainList.isEmpty()) return emptyList()
        val cal = Calendar.getInstance()
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
                iconUrl = "https://openweathermap.org/img/wn/${item.iconCode}.png",
                timeLabel = timeFmt.format(date),
                dateLabel = dayFmt.format(date),
                isNewDay = isNewDay
            )
        }
    }

    private fun mapDailyForecast(domainList: List<DailyForecast>): List<DailyForecastUiModel> {
        return domainList.map { item ->
            DailyForecastUiModel(
                dayLabel = dailyDayFmt.format(Date(item.date * 1000)),
                iconUrl = "https://openweathermap.org/img/wn/${item.iconCode}.png",
                popLabel = "${(item.pop * 100).roundToInt()}%",
                tempMinLabel = "${item.tempMin.roundToInt()}°",
                tempMaxLabel = "${item.tempMax.roundToInt()}°"
            )
        }
    }
}
