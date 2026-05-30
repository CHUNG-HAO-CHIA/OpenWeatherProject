package com.app.openweather.feature.weather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.openweather.core.common.Result
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.DailyForecast
import com.app.openweather.core.domain.usecase.GetCurrentWeatherUseCase
import com.app.openweather.core.domain.usecase.GetWeeklyForecastUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = false,
    val currentWeather: CurrentWeather? = null,
    val weeklyForecast: List<DailyForecast> = emptyList(),
    val error: String? = null,
)

class WeatherViewModel(
    private val getCurrentWeather: GetCurrentWeatherUseCase,
    private val getWeeklyForecast: GetWeeklyForecastUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    fun loadWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            getCurrentWeather(lat, lon).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> _uiState.value.copy(isLoading = true, error = null)
                    is Result.Success -> _uiState.value.copy(isLoading = false, currentWeather = result.data)
                    is Result.Error -> _uiState.value.copy(isLoading = false, error = result.exception.message)
                }
            }
        }
        viewModelScope.launch {
            getWeeklyForecast(lat, lon).collect { result ->
                if (result is Result.Success) {
                    _uiState.value = _uiState.value.copy(weeklyForecast = result.data)
                }
            }
        }
    }
}
