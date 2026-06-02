package com.app.openweather.core.data.repository

import com.app.openweather.core.data.local.dao.WeatherDao
import com.app.openweather.core.domain.model.RawForecastItem
import com.app.openweather.core.network.api.WeatherApi
import com.app.openweather.core.network.api.NominatimApi
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WeatherRepositoryImplTest {

    private val mockApi: WeatherApi = mockk()
    private val mockDao: WeatherDao = mockk()
    private val mockNominatimApi: NominatimApi = mockk()
    private val repository = WeatherRepositoryImpl(mockApi, mockDao, mockNominatimApi)

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun createRawItem(
        dtTxt: String,
        temp: Double,
        tempMin: Double = temp,
        tempMax: Double = temp,
    ): RawForecastItem {
        val dt = sdf.parse(dtTxt)?.time?.div(1000) ?: 0L
        return RawForecastItem(
            dt = dt,
            dtTxt = dtTxt,
            temp = temp,
            tempMin = tempMin,
            tempMax = tempMax,
            feelsLike = temp,
            humidity = 50,
            windSpeed = 5.0,
            pop = 0.0,
            description = "clear sky",
            iconCode = "01d"
        )
    }

    @Test
    fun `calculateForecast should return hourly and daily forecast`() = runTest {
        // Arrange
        val rawItems = listOf(
            // Day 1
            createRawItem("2023-10-27 00:00:00", temp = 15.0, tempMin = 14.0, tempMax = 16.0),
            createRawItem("2023-10-27 09:00:00", temp = 20.0, tempMin = 19.0, tempMax = 21.0),
            createRawItem("2023-10-27 12:00:00", temp = 25.0, tempMin = 24.0, tempMax = 26.0), // Closest to noon
            createRawItem("2023-10-27 18:00:00", temp = 18.0, tempMin = 17.0, tempMax = 19.0),
            // Day 2
            createRawItem("2023-10-28 00:00:00", temp = 10.0, tempMin = 9.0, tempMax = 11.0),
            createRawItem("2023-10-28 15:00:00", temp = 22.0, tempMin = 20.0, tempMax = 24.0), // Closest to noon
            createRawItem("2023-10-28 21:00:00", temp = 15.0, tempMin = 14.0, tempMax = 16.0)
        )

        // Act
        val forecast = repository.calculateForecast(rawItems)

        // Assert Hourly
        assertEquals(7, forecast.hourly.size)
        assertEquals(15.0, forecast.hourly[0].temp, 0.0)

        // Assert Daily
        assertEquals(2, forecast.daily.size)

        // Day 1 Assertions
        val day1 = forecast.daily[0]
        assertEquals(14.0, day1.tempMin, 0.0) // Overall min for day 1
        assertEquals(26.0, day1.tempMax, 0.0) // Overall max for day 1
        // Verify representative data is from 12:00 (Closest to noon)
        val day1NoonDt = sdf.parse("2023-10-27 12:00:00")?.time?.div(1000) ?: 0L
        assertEquals(day1NoonDt, day1.date)

        // Day 2 Assertions
        val day2 = forecast.daily[1]
        assertEquals(9.0, day2.tempMin, 0.0) // Overall min for day 2
        assertEquals(24.0, day2.tempMax, 0.0) // Overall max for day 2
        // Verify representative data is from 15:00 (Closest to noon available)
        val day2NoonDt = sdf.parse("2023-10-28 15:00:00")?.time?.div(1000) ?: 0L
        assertEquals(day2NoonDt, day2.date)
    }

    @Test
    fun `calculateForecast should take max 24 items for hourly`() = runTest {
        // Arrange
        val rawItems = (1..30).map { i ->
            val day = if (i <= 8) "27" else if (i <= 16) "28" else "29"
            val hour = String.format("%02d", (i * 3) % 24)
            createRawItem("2023-10-$day $hour:00:00", temp = 20.0)
        }

        // Act
        val forecast = repository.calculateForecast(rawItems)

        // Assert Hourly
        assertEquals(24, forecast.hourly.size)
    }
}
