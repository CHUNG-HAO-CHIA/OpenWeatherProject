package com.app.openweather.feature.city.viewmodel

import androidx.lifecycle.ViewModel
import com.app.openweather.core.domain.model.City
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CityViewModel : ViewModel() {

    private val _cities = MutableStateFlow(
        listOf(
            City("Taipei", "TW", 25.0478, 121.5318),
            City("Tokyo", "JP", 35.6895, 139.6917),
            City("London", "GB", 51.5074, -0.1278),
            City("New York", "US", 40.7128, -74.0060),
            City("Paris", "FR", 48.8566, 2.3522),
            City("Sydney", "AU", -33.8688, 151.2093),
        )
    )
    val cities: StateFlow<List<City>> = _cities.asStateFlow()
}
