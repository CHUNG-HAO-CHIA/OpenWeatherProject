package com.app.openweather.feature.weather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.model.Forecast
import com.app.openweather.core.domain.model.HourlyForecast
import com.app.openweather.core.domain.usecase.GetCurrentWeatherUseCase
import com.app.openweather.core.domain.usecase.GetForecastUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = false,
    val currentWeather: CurrentWeather? = null,
    val hourlyForecast: List<HourlyForecast> = emptyList(),
    val weeklyForecast: List<DailyForecast> = emptyList(),
    val error: String? = null,
)

class WeatherViewModel(
    private val getCurrentWeather: GetCurrentWeatherUseCase,
    private val getForecast: GetForecastUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadWeather(lat: Double, lon: Double) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getCurrentWeather(lat, lon),
                getForecast(lat, lon),
            ) { weatherResult, forecastResult ->
                Pair(weatherResult, forecastResult)
            }.collect { (weatherResult, forecastResult) ->
                _uiState.update { state ->
                    var next = state

                    next = when (weatherResult) {
                        is Result.Loading -> next.copy(isLoading = true, error = null)
                        is Result.Success -> next.copy(isLoading = false, currentWeather = weatherResult.data)
                        is Result.Error -> next.copy(isLoading = false, error = weatherResult.exception.message)
                    }

                    next = when (forecastResult) {
                        is Result.Success -> next.copy(
                            hourlyForecast = forecastResult.data.hourly,
                            weeklyForecast = forecastResult.data.daily,
                        )
                        is Result.Error -> next.copy(error = forecastResult.exception.message)
                        is Result.Loading -> next
                    }

                    next
                }
            }
        }
    }
}
