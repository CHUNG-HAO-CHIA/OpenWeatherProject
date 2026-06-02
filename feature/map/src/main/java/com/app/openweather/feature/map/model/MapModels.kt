package com.app.openweather.feature.map.model

import com.app.openweather.core.domain.model.SavedCity

data class MapMarkerUiModel(
    val cityId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val tempLabel: String,
    val iconCode: String,
    val isRainy: Boolean,
)

sealed interface LocationPreviewUiState {
    object Idle : LocationPreviewUiState
    object Loading : LocationPreviewUiState
    data class Success(
        val city: SavedCity,
        val tempLabel: String,
        val description: String,
        val iconCode: String,
    ) : LocationPreviewUiState
    data class Error(val message: String) : LocationPreviewUiState
}
