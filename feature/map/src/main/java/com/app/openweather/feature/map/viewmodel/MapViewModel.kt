package com.app.openweather.feature.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.usecase.CityUseCases
import com.app.openweather.core.domain.usecase.WeatherUseCases
import com.app.openweather.feature.map.model.MapMarkerUiModel
import com.app.openweather.feature.map.model.LocationPreviewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class MapUiState(
    val markers: List<MapMarkerUiModel> = emptyList(),
    val locationPreview: LocationPreviewUiState = LocationPreviewUiState.Idle,
    val isMarkersLoading: Boolean = false,
)

class MapViewModel(
    private val cityUseCases: CityUseCases,
    private val weatherUseCases: WeatherUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadSavedCitiesWithWeather()
    }

    private fun loadSavedCitiesWithWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMarkersLoading = true) }
            cityUseCases.getSavedCities().collect { cities ->
                val weatherMap = weatherUseCases.getWeatherForCities(cities)
                val markers = cities.mapNotNull { city ->
                    val weather = weatherMap[city.id] ?: return@mapNotNull null
                    MapMarkerUiModel(
                        cityId = city.id,
                        name = city.name,
                        lat = city.lat,
                        lon = city.lon,
                        tempLabel = "${weather.temperature.roundToInt()}°",
                        iconUrl = "https://openweathermap.org/img/wn/${weather.iconCode}.png",
                        isRainy = weather.description.contains("rain", ignoreCase = true)
                    )
                }
                _uiState.update { it.copy(markers = markers, isMarkersLoading = false) }
            }
        }
    }

    fun onMapClick(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(locationPreview = LocationPreviewUiState.Loading) }
            try {
                val city = cityUseCases.reverseGeocode(lat, lon)
                val weatherMap = weatherUseCases.getWeatherForCities(listOf(city))
                val weather = weatherMap[city.id]
                
                if (weather != null) {
                    _uiState.update {
                        it.copy(
                            locationPreview = LocationPreviewUiState.Success(
                                city = city,
                                tempLabel = "${weather.temperature.roundToInt()}°C",
                                description = weather.description.replaceFirstChar { it.uppercase() },
                                iconUrl = "https://openweathermap.org/img/wn/${weather.iconCode}@2x.png"
                            )
                        )
                    }
                } else {
                    _uiState.update { it.copy(locationPreview = LocationPreviewUiState.Error("無法獲取天氣資料")) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(locationPreview = LocationPreviewUiState.Error("無法獲取位置資訊")) }
            }
        }
    }

    fun dismissPreview() {
        _uiState.update { it.copy(locationPreview = LocationPreviewUiState.Idle) }
    }
}
