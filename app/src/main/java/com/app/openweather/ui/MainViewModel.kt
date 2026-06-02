package com.app.openweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.usecase.GetSavedCitiesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

private const val DEFAULT_LAT = 25.0478
private const val DEFAULT_LON = 121.5318

data class NavigationState(
    val lat: Double = DEFAULT_LAT,
    val lon: Double = DEFAULT_LON,
    val locationReady: Boolean = true,
)

class MainViewModel(
    getSavedCities: GetSavedCitiesUseCase,
) : ViewModel() {

    val favoriteCities: StateFlow<List<SavedCity>> = getSavedCities()
        .map { cities -> cities.filter { it.isFavorite } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _navState = MutableStateFlow(NavigationState())
    val navState: StateFlow<NavigationState> = _navState.asStateFlow()

    fun updateLocation(lat: Double, lon: Double, cityName: String? = null) {
        _navState.update { it.copy(lat = lat, lon = lon, locationReady = true,) }
    }

    fun setLocationReady() {
        _navState.update { it.copy(locationReady = true) }
    }
}
