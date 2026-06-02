package com.app.openweather.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.app.openweather.core.R

@DrawableRes
fun iconCodeToDrawable(iconCode: String): Int = when (iconCode) {
    "01d" -> R.drawable.w_01d
    "01n" -> R.drawable.w_01n
    "02d" -> R.drawable.w_02d
    "02n" -> R.drawable.w_02n
    "03d" -> R.drawable.w_03d
    "03n" -> R.drawable.w_03n
    "04d" -> R.drawable.w_04d
    "04n" -> R.drawable.w_04n
    "09d" -> R.drawable.w_09d
    "09n" -> R.drawable.w_09n
    "10d" -> R.drawable.w_10d
    "10n" -> R.drawable.w_10n
    "11d" -> R.drawable.w_11d
    "11n" -> R.drawable.w_11n
    "13d" -> R.drawable.w_13d
    "13n" -> R.drawable.w_13n
    "50d" -> R.drawable.w_50d
    "50n" -> R.drawable.w_50n
    else -> R.drawable.w_01d
}

@Composable
fun WeatherIcon(
    iconCode: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(iconCodeToDrawable(iconCode)),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
