package com.app.openweather.feature.city.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.openweather.core.domain.model.SavedCity
import com.app.openweather.core.domain.usecase.CityUseCases
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

data class CityUiState(
    val savedCities: List<SavedCity> = emptyList(),
    val searchResults: List<SavedCity> = emptyList(),
    val query: String = "",
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(FlowPreview::class)
class CityViewModel(
    private val useCases: CityUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CityUiState())
    val uiState: StateFlow<CityUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            useCases.getSavedCities().collect { cities ->
                _uiState.value = _uiState.value.copy(savedCities = cities)
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

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query, errorMessage = null)
        _queryFlow.value = query
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            try {
                val results = useCases.searchCities(query)
                _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSearching = false, errorMessage = "搜尋失敗，請稍後再試")
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
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun onDeleteCity(cityId: String) {
        viewModelScope.launch { useCases.deleteCity(cityId) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
