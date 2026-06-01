package com.app.openweather.core.domain.usecase

import com.app.openweather.core.domain.repository.CityRepository

const val MAX_FAVORITES = 3

class MaxFavoritesReachedException : Exception("最多只能加星 $MAX_FAVORITES 個地點")

class ToggleFavoriteCityUseCase(private val repository: CityRepository) {
    suspend operator fun invoke(cityId: String): Result<Unit> {
        val city = repository.getCityById(cityId) ?: return Result.failure(Exception("City not found"))
        
        return if (city.isFavorite) {
            repository.setFavorite(cityId, false)
            Result.success(Unit)
        } else {
            val count = repository.getFavoriteCount()
            if (count >= MAX_FAVORITES) {
                Result.failure(MaxFavoritesReachedException())
            } else {
                repository.setFavorite(cityId, true)
                Result.success(Unit)
            }
        }
    }
}
