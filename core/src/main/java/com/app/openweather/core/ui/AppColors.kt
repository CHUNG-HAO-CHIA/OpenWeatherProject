package com.app.openweather.core.ui

import androidx.compose.ui.graphics.Color

object AppColors {
    val BgDark = Color(0xFF1B2033)
    val BgCard = Color(0xFF252B3E)
    val BgHighlight = Color(0xFF2E3650)
    val AccentBlue = Color(0xFF5B9CF6)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFABB3C9)
    val ChartLine = Color(0xFFF5C842)
    val StarColor = Color(0xFFF5C842)
    val Danger = Color(0xFFFF6B6B)

    // Weather gradient top/bottom pairs — keyed by OpenWeatherMap icon code
    fun weatherGradient(iconCode: String): Pair<Color, Color> {
        val prefix = iconCode.take(2)
        val isDay = iconCode.endsWith("d")
        return when (prefix) {
            "01" -> if (isDay)
                Color(0xFF1A4A7C) to Color(0xFFD4843A)   // clear day
            else
                Color(0xFF080D1E) to Color(0xFF1A2340)   // clear night
            "02" -> if (isDay)
                Color(0xFF285A90) to Color(0xFF638EB4)   // few clouds: bright & airy
            else
                Color(0xFF0F1626) to Color(0xFF1B263B)
            "03" -> if (isDay)
                Color(0xFF1C3450) to Color(0xFF4A6580)   // scattered clouds: neutral blue-grey
            else
                Color(0xFF0E1525) to Color(0xFF1E2B3D)
            "04" -> if (isDay)
                Color(0xFF1A2535) to Color(0xFF374960)   // broken/overcast: heavy slate grey
            else
                Color(0xFF0C1018) to Color(0xFF1A2230)
            "09", "10" -> if (isDay)
                Color(0xFF0F1E30) to Color(0xFF1C3248)   // rain day: dark slate
            else
                Color(0xFF080E18) to Color(0xFF101B28)   // rain night
            "11" -> Color(0xFF080A18) to Color(0xFF100E28) // thunderstorm: dark purple-black
            "13" -> if (isDay)
                Color(0xFF1E3050) to Color(0xFF8BAEC8)   // snow day: ice blue fade
            else
                Color(0xFF0E1828) to Color(0xFF2C4060)   // snow night
            "50" -> if (isDay)
                Color(0xFF1A2535) to Color(0xFF5A6A7A)   // mist day: hazy grey-blue
            else
                Color(0xFF0E1520) to Color(0xFF2A3545)   // mist night
            else -> Color(0xFF1B2033) to Color(0xFF252B3E) // fallback
        }
    }
}
