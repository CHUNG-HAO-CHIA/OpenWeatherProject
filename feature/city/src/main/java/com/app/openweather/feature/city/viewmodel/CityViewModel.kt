package com.app.openweather.feature.city.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.openweather.core.common.AppError
import com.app.openweather.core.common.toAppError
import com.app.openweather.core.domain.model.CurrentWeather
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.usecase.CityUseCases
import com.app.openweather.core.domain.usecase.GetWeatherForCitiesUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CityUiState(
    val favoriteCities: List<SavedCity> = emptyList(),
    val otherCities: List<SavedCity> = emptyList(),
    val searchResults: List<SavedCity> = emptyList(),
    val cityWeather: Map<String, CurrentWeather> = emptyMap(),
    val query: String = "",
    val isSearching: Boolean = false,
    val error: AppError? = null,
) {
    val savedCities: List<SavedCity> get() = favoriteCities + otherCities
}

@OptIn(FlowPreview::class)
class CityViewModel(
    private val useCases: CityUseCases,
    private val getWeatherForCities: GetWeatherForCitiesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CityUiState())
    val uiState: StateFlow<CityUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            useCases.getSavedCities().collect { cities ->
                _uiState.update {
                    it.copy(
                        favoriteCities = cities.filter { c -> c.isFavorite },
                        otherCities = cities.filter { c -> !c.isFavorite },
                    )
                }
                refreshWeather(cities)
            }
        }
        viewModelScope.launch {
            _queryFlow
                .debounce(400)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query -> performSearch(query) }
        }
    }

    private fun refreshWeather(cities: List<SavedCity>) {
        if (cities.isEmpty()) {
            _uiState.update { it.copy(cityWeather = emptyMap()) }
            return
        }
        viewModelScope.launch {
            val weatherMap = getWeatherForCities(cities)
            // Replace entirely — never accumulate stale entries from deleted cities
            _uiState.update { it.copy(cityWeather = weatherMap) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update {
            if (query.length < 2) it.copy(query = query, error = null, searchResults = emptyList(), isSearching = false)
            else it.copy(query = query)
        }
        _queryFlow.value = query
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = useCases.searchCities(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, error = AppError.UnknownError) }
            }
        }
    }

    fun onSaveCity(city: SavedCity) {
        viewModelScope.launch { useCases.saveCity(city) }
    }

    fun onToggleFavorite(cityId: String) {
        viewModelScope.launch {
            val result = useCases.toggleFavorite(cityId)
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.toAppError()) }
            }
        }
    }

    fun onDeleteCity(cityId: String) {
        // Optimistic: immediately evict stale weather entry before DB confirms
        _uiState.update { it.copy(cityWeather = it.cityWeather - cityId) }
        viewModelScope.launch { useCases.deleteCity(cityId) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
