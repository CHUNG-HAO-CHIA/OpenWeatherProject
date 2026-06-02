package com.app.openweather.core.common

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Offline<T>(val cachedData: T, val cachedAt: Long) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
