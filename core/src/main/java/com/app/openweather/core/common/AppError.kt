package com.app.openweather.core.common

import android.content.Context
import com.app.openweather.core.R

sealed interface AppError {
    data object NetworkError : AppError
    data class ApiError(val code: Int, val message: String?) : AppError
    data class LocationError(val message: String?) : AppError
    data object UnknownError : AppError
}

fun AppError.toUserMessage(context: Context): String = when (this) {
    is AppError.NetworkError -> context.getString(R.string.error_network)
    is AppError.ApiError -> context.getString(R.string.error_api, code)
    is AppError.LocationError -> context.getString(R.string.error_location)
    is AppError.UnknownError -> context.getString(R.string.error_unknown)
}

fun Throwable.toAppError(): AppError = when (this) {
    is java.io.IOException -> AppError.NetworkError
    is retrofit2.HttpException -> AppError.ApiError(code(), message())
    is SecurityException -> AppError.LocationError(message)
    else -> AppError.UnknownError
}
