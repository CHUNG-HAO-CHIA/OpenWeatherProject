package com.app.openweather.core.domain.repository

import com.app.openweather.core.domain.model.SavedCity
import kotlinx.coroutines.flow.Flow

interface CityRepository {
    fun getSavedCities(): Flow<List<SavedCity>>
    suspend fun searchCities(query: String): List<SavedCity>
    suspend fun saveCity(city: SavedCity)
    suspend fun toggleFavorite(cityId: String): Result<Unit>
    suspend fun deleteCity(cityId: String)
}
